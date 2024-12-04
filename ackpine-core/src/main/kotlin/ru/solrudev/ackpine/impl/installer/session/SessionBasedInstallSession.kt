/*
 * Copyright (C) 2023-2024 Ilya Fomichev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:SuppressLint("LongLogTag")

package ru.solrudev.ackpine.impl.installer.session

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageInstaller
import android.content.pm.PackageInstaller.SessionParams.MODE_FULL_INSTALL
import android.content.pm.PackageInstaller.SessionParams.MODE_INHERIT_EXISTING
import android.content.pm.PackageManager
import android.content.res.AssetFileDescriptor
import android.graphics.BitmapFactory
import android.icu.util.ULocale
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.OperationCanceledException
import android.os.Process
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.core.content.edit
import ru.solrudev.ackpine.helpers.concurrent.BinarySemaphore
import ru.solrudev.ackpine.helpers.concurrent.executeWithSemaphore
import ru.solrudev.ackpine.helpers.concurrent.handleResult
import ru.solrudev.ackpine.helpers.concurrent.withPermit
import ru.solrudev.ackpine.impl.activity.SessionCommitActivity
import ru.solrudev.ackpine.impl.database.dao.InstallConstraintsDao
import ru.solrudev.ackpine.impl.database.dao.InstallPreapprovalDao
import ru.solrudev.ackpine.impl.database.dao.NativeSessionIdDao
import ru.solrudev.ackpine.impl.database.dao.SessionDao
import ru.solrudev.ackpine.impl.database.dao.SessionFailureDao
import ru.solrudev.ackpine.impl.database.dao.SessionProgressDao
import ru.solrudev.ackpine.impl.installer.receiver.PackageInstallerStatusReceiver
import ru.solrudev.ackpine.impl.installer.session.helpers.PROGRESS_MAX
import ru.solrudev.ackpine.impl.installer.session.helpers.copyTo
import ru.solrudev.ackpine.impl.installer.session.helpers.openAssetFileDescriptor
import ru.solrudev.ackpine.impl.session.AbstractProgressSession
import ru.solrudev.ackpine.impl.session.helpers.UPDATE_CURRENT_FLAGS
import ru.solrudev.ackpine.installer.InstallFailure
import ru.solrudev.ackpine.installer.InstallFailure.Timeout
import ru.solrudev.ackpine.installer.parameters.InstallConstraints
import ru.solrudev.ackpine.installer.parameters.InstallConstraints.Companion.NONE
import ru.solrudev.ackpine.installer.parameters.InstallConstraints.TimeoutStrategy
import ru.solrudev.ackpine.installer.parameters.InstallMode
import ru.solrudev.ackpine.installer.parameters.InstallPreapproval
import ru.solrudev.ackpine.installer.parameters.PackageSource
import ru.solrudev.ackpine.session.Progress
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.session.Session.State.Committed
import ru.solrudev.ackpine.session.Session.State.Completed
import ru.solrudev.ackpine.session.Session.State.Failed
import ru.solrudev.ackpine.session.Session.State.Succeeded
import ru.solrudev.ackpine.session.parameters.Confirmation
import ru.solrudev.ackpine.session.parameters.NotificationData
import java.util.UUID
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random
import kotlin.random.nextInt

private const val ACKPINE_SESSION_BASED_INSTALLER = "ackpine_session_based_installer"
private const val SESSION_COMMIT_PROGRESS_VALUE = "session_commit_progress_value"
private const val TAG = "SessionBasedInstallSession"

@RestrictTo(RestrictTo.Scope.LIBRARY)
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
internal class SessionBasedInstallSession internal constructor(
	private val context: Context,
	private val apks: List<Uri>,
	id: UUID,
	initialState: Session.State<InstallFailure>,
	initialProgress: Progress,
	private val confirmation: Confirmation,
	private val notificationData: NotificationData,
	private val requireUserAction: Boolean,
	private val installMode: InstallMode,
	private val preapproval: InstallPreapproval,
	private val constraints: InstallConstraints,
	private val requestUpdateOwnership: Boolean,
	private val packageSource: PackageSource,
	sessionDao: SessionDao,
	sessionFailureDao: SessionFailureDao<InstallFailure>,
	sessionProgressDao: SessionProgressDao,
	private val nativeSessionIdDao: NativeSessionIdDao,
	private val installPreapprovalDao: InstallPreapprovalDao,
	private val installConstraintsDao: InstallConstraintsDao,
	private val executor: Executor,
	private val handler: Handler,
	notificationId: Int,
	commitAttemptsCount: Int,
	@Volatile private var isPreapproved: Boolean,
	private val dbWriteSemaphore: BinarySemaphore
) : AbstractProgressSession<InstallFailure>(
	context, id, initialState, initialProgress,
	sessionDao, sessionFailureDao, sessionProgressDao,
	executor, handler,
	exceptionalFailureFactory = InstallFailure::Exceptional,
	notificationId, dbWriteSemaphore
), PreapprovalListener {

	@Volatile
	private var nativeSessionId = -1

	@Volatile
	private var sessionCallback: PackageInstaller.SessionCallback? = null

	@Volatile
	private var isPreapprovalActive = false

	private val nativeSessionIdSemaphore = BinarySemaphore()
	private val attempts = AtomicInteger(commitAttemptsCount)

	init {
		completeIfSucceeded(initialState, initialProgress)
	}

	private fun completeIfSucceeded(initialState: Session.State<InstallFailure>, initialProgress: Progress) {
		if (initialState.isTerminal) {
			return
		}
		if (initialProgress.progress >= (context.getSessionBasedSessionCommitProgressValue() * PROGRESS_MAX).toInt()) {
			// means that actual installation is ongoing or is completed
			notifyCommitted() // block clients from committing
		}
		executor.executeWithSemaphore(nativeSessionIdSemaphore) {
			val nativeSessionId = nativeSessionIdDao.getNativeSessionId(id.toString()) ?: -1
			this.nativeSessionId = nativeSessionId
			if (nativeSessionId != -1) {
				sessionCallback = packageInstaller.createAndRegisterSessionCallback(nativeSessionId)
			}
			// If app is killed while installing but system installer activity remains visible,
			// session is stuck in Committed state after new process start.
			// Fails are guaranteed to be handled by PackageInstallerStatusReceiver (in case of self-update
			// success is not handled), so if native session doesn't exist, it can only mean that it succeeded.
			// There may be latency from the receiver, so we delay this to allow the receiver to kick in.
			if (initialState is Committed && packageInstaller.getSessionInfo(nativeSessionId) == null) {
				handler.postDelayed({ complete(Succeeded) }, 2000)
			}
		}
	}

	private val packageInstaller: PackageInstaller
		get() = context.packageManager.packageInstaller

	override fun prepare() {
		if (isPreapprovalActive) {
			return
		}
		val sessionId = getSessionId()
		if (isPreapprovalIgnored() || isPreapproved) {
			writeApksToSession(sessionId)
			return
		}
		val preapprovalDetails = getPackageInstallerPreapprovalDetails()
		packageInstaller.openSession(sessionId).requestUserPreapproval(
			preapprovalDetails,
			getPackageInstallerStatusIntentSender()
		)
	}

	override fun launchConfirmation() {
		if (isInstallConstraintsIgnored() || shouldCommitNormallyAfterTimeout()) {
			commitPackageInstallerSession()
		} else try {
			commitPackageInstallerSessionWithConstraints()
		} catch (exception: SecurityException) {
			Log.w(TAG, "$id: ${exception.message}")
			commitPackageInstallerSession()
		}
		val currentAttempt = attempts.incrementAndGet()
		notifyCommitted()
		dbWriteSemaphore.withPermit {
			installConstraintsDao.setCommitAttemptsCount(id.toString(), currentAttempt)
		}
	}

	override fun onPreapproval() {
		isPreapprovalActive = true
	}

	override fun onPreapproved() {
		isPreapproved = true
		isPreapprovalActive = false
		executor.execute {
			dbWriteSemaphore.withPermit {
				installPreapprovalDao.setPreapproved(id.toString())
			}
		}
		executor.execute {
			try {
				writeApksToSession(nativeSessionId)
			} catch (_: OperationCanceledException) {
				cancel()
			} catch (exception: Exception) {
				completeExceptionally(exception)
			}
		}
	}

	override fun onCompleted(state: Completed<InstallFailure>): Boolean {
		if (isInstallConstraintsIgnored() || constraints.timeoutStrategy == TimeoutStrategy.Fail) {
			return true
		}
		if (state !is Failed || state.failure !is Timeout) {
			return true
		}
		if (shouldCommitNormallyAfterTimeout()) {
			notifyAwaiting()
			return false
		}
		if (constraints.timeoutStrategy is TimeoutStrategy.Retry) {
			val currentAttempt = attempts.get()
			val shouldRetry = currentAttempt <= constraints.timeoutStrategy.retries
			if (shouldRetry) {
				Log.i(TAG, "Retrying $id: attempt #$currentAttempt")
				notifyAwaiting()
			}
			return !shouldRetry
		}
		return true
	}

	override fun doCleanup() {
		executor.execute(::abandonSession) // may be long if storage is under load
		packageInstaller.clearSessionCallback()
		isPreapprovalActive = false
	}

	private fun isPreapprovalIgnored(): Boolean {
		return preapproval == InstallPreapproval.NONE || Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE
	}

	@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
	private fun getPackageInstallerPreapprovalDetails(): PackageInstaller.PreapprovalDetails {
		val icon = if (preapproval.icon != Uri.EMPTY) {
			context.contentResolver.openInputStream(preapproval.icon)?.use { iconStream ->
				iconStream.buffered().use { bufferedIconStream ->
					BitmapFactory.decodeStream(bufferedIconStream)
				}
			}
		} else {
			null
		}
		val builder = PackageInstaller.PreapprovalDetails.Builder()
			.setPackageName(preapproval.packageName)
			.setLabel(preapproval.label)
			.setLocale(ULocale.forLanguageTag(preapproval.languageTag))
		if (icon != null) {
			builder.setIcon(icon)
		}
		return builder.build()
	}

	private fun isInstallConstraintsIgnored(): Boolean {
		return constraints == NONE || Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE
	}

	private fun shouldCommitNormallyAfterTimeout(): Boolean {
		return constraints.timeoutStrategy == TimeoutStrategy.CommitEagerly && attempts.get() == 1
	}

	private fun commitPackageInstallerSession() {
		val statusReceiver = getPackageInstallerStatusIntentSender()
		val sessionId = nativeSessionId
		if (packageInstaller.getSessionInfo(sessionId) != null) {
			packageInstaller.openSession(sessionId).commit(statusReceiver)
			writeCommitProgressIfAbsent()
		}
	}

	@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
	private fun commitPackageInstallerSessionWithConstraints() {
		val statusReceiver = getPackageInstallerStatusIntentSender()
		val sessionId = nativeSessionId
		if (packageInstaller.getSessionInfo(sessionId) != null) {
			val installConstraints = getPackageInstallerInstallConstraints()
			packageInstaller.commitSessionAfterInstallConstraintsAreMet(
				sessionId, statusReceiver, installConstraints, constraints.timeoutMillis
			)
			writeCommitProgressIfAbsent()
		}
	}

	private fun getPackageInstallerStatusIntentSender(): IntentSender {
		val receiverIntent = Intent(context, PackageInstallerStatusReceiver::class.java).apply {
			action = PackageInstallerStatusReceiver.getAction(context)
			putExtra(SessionCommitActivity.EXTRA_ACKPINE_SESSION_ID, id)
			putExtra(PackageInstallerStatusReceiver.EXTRA_CONFIRMATION, confirmation.ordinal)
			putExtra(PackageInstallerStatusReceiver.EXTRA_NOTIFICATION_ID, notificationId)
			putExtra(PackageInstallerStatusReceiver.EXTRA_NOTIFICATION_TITLE, notificationData.title)
			putExtra(PackageInstallerStatusReceiver.EXTRA_NOTIFICATION_MESSAGE, notificationData.contentText)
			putExtra(PackageInstallerStatusReceiver.EXTRA_NOTIFICATION_ICON, notificationData.icon)
			addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
		}
		val receiverPendingIntent = PendingIntent.getBroadcast(
			context,
			generateRequestCode(),
			receiverIntent,
			UPDATE_CURRENT_FLAGS
		)
		return receiverPendingIntent.intentSender
	}

	@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
	private fun getPackageInstallerInstallConstraints() = PackageInstaller.InstallConstraints.Builder().apply {
		if (constraints.isAppNotForegroundRequired) {
			setAppNotForegroundRequired()
		}
		if (constraints.isAppNotInteractingRequired) {
			setAppNotForegroundRequired()
		}
		if (constraints.isAppNotTopVisibleRequired) {
			setAppNotTopVisibleRequired()
		}
		if (constraints.isDeviceIdleRequired) {
			setDeviceIdleRequired()
		}
		if (constraints.isNotInCallRequired) {
			setNotInCallRequired()
		}
	}.build()

	private fun writeCommitProgressIfAbsent() {
		val preferences = context.getSharedPreferences(ACKPINE_SESSION_BASED_INSTALLER, MODE_PRIVATE)
		if (!preferences.contains(SESSION_COMMIT_PROGRESS_VALUE)) {
			preferences.edit {
				putFloat(
					SESSION_COMMIT_PROGRESS_VALUE,
					packageInstaller.getSessionInfo(nativeSessionId)!!.progress + 0.01f
				)
			}
		}
	}

	private fun getSessionId(): Int {
		val isSessionCreated = nativeSessionIdSemaphore.withPermit { nativeSessionId != -1 }
		return when {
			isPreapprovalIgnored() -> {
				if (isSessionCreated) {
					abandonSession()
					packageInstaller.clearSessionCallback()
				}
				createSession()
			}

			isSessionCreated -> nativeSessionId
			else -> createSession()
		}
	}

	private fun createSession(): Int {
		val sessionId = packageInstaller.createSession(createSessionParams())
		nativeSessionId = sessionId
		persistNativeSessionId(sessionId)
		sessionCallback = packageInstaller.createAndRegisterSessionCallback(sessionId)
		return sessionId
	}

	private fun createSessionParams(): PackageInstaller.SessionParams {
		val sessionParams = when (installMode) {
			is InstallMode.Full -> PackageInstaller.SessionParams(MODE_FULL_INSTALL)
			is InstallMode.InheritExisting -> PackageInstaller.SessionParams(MODE_INHERIT_EXISTING).apply {
				setAppPackageName(installMode.packageName)
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
					setDontKillApp(installMode.dontKillApp)
				}
			}
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			sessionParams.setOriginatingUid(Process.myUid())
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			sessionParams.setInstallReason(PackageManager.INSTALL_REASON_USER)
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
			val requireUserAction = if (requireUserAction) {
				PackageInstaller.SessionParams.USER_ACTION_REQUIRED
			} else {
				PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED
			}
			sessionParams.setRequireUserAction(requireUserAction)
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			sessionParams.setPackageSource(packageSource.toPackageInstallerPackageSource())
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
			sessionParams.setRequestUpdateOwnership(requestUpdateOwnership)
		}
		return sessionParams
	}

	private fun writeApksToSession(sessionId: Int) {
		val session = packageInstaller.openSession(sessionId)
		session.writeApks().handleResult(
			onException = { exception ->
				session.close()
				if (exception is OperationCanceledException) {
					cancel()
				} else {
					completeExceptionally(exception)
				}
			},
			block = {
				session.close()
				notifyAwaiting()
			})
	}

	private fun PackageInstaller.Session.writeApks() = CallbackToFutureAdapter.getFuture { completer ->
		val countdown = AtomicInteger(apks.size)
		val currentProgress = AtomicInteger(0)
		val progressMax = apks.size * PROGRESS_MAX
		apks.forEachIndexed { index, uri ->
			val afd = context.openAssetFileDescriptor(uri, cancellationSignal)
				?: error("AssetFileDescriptor was null: $uri")
			try {
				executor.execute {
					try {
						writeApk(afd, index, currentProgress, progressMax)
						if (countdown.decrementAndGet() == 0) {
							completer.set(Unit)
						}
					} catch (t: Throwable) {
						completer.setException(t)
					} finally {
						afd.close()
					}
				}
			} catch (t: Throwable) {
				completer.setException(t)
				afd.close()
			}
		}
		"SessionBasedInstallSession.writeApks"
	}

	private fun PackageInstaller.Session.writeApk(
		afd: AssetFileDescriptor,
		index: Int,
		currentProgress: AtomicInteger,
		progressMax: Int
	) = afd.createInputStream().use { apkStream ->
		requireNotNull(apkStream) { "APK $index InputStream was null." }
		val length = afd.declaredLength
		val sessionStream = openWrite("$index.apk", 0, length)
		sessionStream.buffered().use { bufferedSessionStream ->
			apkStream.copyTo(bufferedSessionStream, length, cancellationSignal, onProgress = { progress ->
				val current = currentProgress.addAndGet(progress)
				setStagingProgress(current.toFloat() / progressMax)
			})
			bufferedSessionStream.flush()
			fsync(sessionStream)
		}
	}

	private fun PackageInstaller.createAndRegisterSessionCallback(
		nativeSessionId: Int
	): PackageInstaller.SessionCallback {
		val callback = packageInstallerSessionCallback(nativeSessionId)
		handler.post {
			registerSessionCallback(callback)
		}
		return callback
	}

	private fun PackageInstaller.clearSessionCallback() {
		val callback = sessionCallback ?: return
		sessionCallback = null
		handler.post {
			unregisterSessionCallback(callback)
		}
	}

	private fun packageInstallerSessionCallback(nativeSessionId: Int) = object : PackageInstaller.SessionCallback() {
		override fun onCreated(sessionId: Int) { /* no-op */ }
		override fun onBadgingChanged(sessionId: Int) { /* no-op */ }
		override fun onActiveChanged(sessionId: Int, active: Boolean) { /* no-op */ }
		override fun onFinished(sessionId: Int, success: Boolean) { /* no-op */ }

		override fun onProgressChanged(sessionId: Int, progress: Float) {
			if (sessionId == nativeSessionId) {
				setProgress((progress * PROGRESS_MAX).toInt())
			}
		}
	}

	private fun abandonSession() {
		try {
			packageInstaller.abandonSession(nativeSessionId)
		} catch (_: Throwable) { // no-op
		}
	}

	private fun persistNativeSessionId(nativeSessionId: Int) = dbWriteSemaphore.withPermit {
		nativeSessionIdDao.setNativeSessionId(id.toString(), nativeSessionId)
	}

	private fun generateRequestCode() = Random.nextInt(10000..1000000)
}

@JvmSynthetic
internal fun Context.getSessionBasedSessionCommitProgressValue(): Float {
	return getSharedPreferences(ACKPINE_SESSION_BASED_INSTALLER, MODE_PRIVATE)
		.getFloat(SESSION_COMMIT_PROGRESS_VALUE, 1f)
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun PackageSource.toPackageInstallerPackageSource() = when (this) {
	PackageSource.Unspecified -> PackageInstaller.PACKAGE_SOURCE_UNSPECIFIED
	PackageSource.Store -> PackageInstaller.PACKAGE_SOURCE_STORE
	PackageSource.LocalFile -> PackageInstaller.PACKAGE_SOURCE_LOCAL_FILE
	PackageSource.DownloadedFile -> PackageInstaller.PACKAGE_SOURCE_DOWNLOADED_FILE
	PackageSource.Other -> PackageInstaller.PACKAGE_SOURCE_OTHER
	else -> PackageInstaller.PACKAGE_SOURCE_UNSPECIFIED
}