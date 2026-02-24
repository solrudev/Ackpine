/*
 * Copyright (C) 2023 Ilya Fomichev
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
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageInstaller
import android.content.pm.PackageInstaller.SessionParams.MODE_FULL_INSTALL
import android.content.pm.PackageInstaller.SessionParams.MODE_INHERIT_EXISTING
import android.content.pm.PackageManager
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.icu.util.ULocale
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import android.os.Handler
import android.os.OperationCanceledException
import android.util.Log
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.concurrent.futures.CallbackToFutureAdapter
import ru.solrudev.ackpine.helpers.closeAllWithException
import ru.solrudev.ackpine.helpers.closeWithException
import ru.solrudev.ackpine.helpers.concurrent.handleResult
import ru.solrudev.ackpine.helpers.getOrElse
import ru.solrudev.ackpine.helpers.mapCatchingFirst
import ru.solrudev.ackpine.helpers.use
import ru.solrudev.ackpine.impl.database.dao.InstallConstraintsDao
import ru.solrudev.ackpine.impl.database.dao.InstallPreapprovalDao
import ru.solrudev.ackpine.impl.database.dao.NativeSessionIdDao
import ru.solrudev.ackpine.impl.database.dao.SessionDao
import ru.solrudev.ackpine.impl.database.dao.SessionFailureDao
import ru.solrudev.ackpine.impl.database.dao.SessionProgressDao
import ru.solrudev.ackpine.impl.helpers.concurrent.BinarySemaphore
import ru.solrudev.ackpine.impl.helpers.concurrent.withPermit
import ru.solrudev.ackpine.impl.helpers.createPackageInstallerStatusIntentSender
import ru.solrudev.ackpine.impl.installer.CommitProgressValueHolder
import ru.solrudev.ackpine.impl.installer.receiver.PackageInstallerStatusReceiver
import ru.solrudev.ackpine.impl.installer.session.helpers.PROGRESS_MAX
import ru.solrudev.ackpine.impl.installer.session.helpers.copyTo
import ru.solrudev.ackpine.impl.installer.session.helpers.openAssetFileDescriptor
import ru.solrudev.ackpine.impl.receiver.SystemPackageInstallerStatusReceiver
import ru.solrudev.ackpine.impl.services.PackageInstallerService
import ru.solrudev.ackpine.impl.session.AbstractProgressSession
import ru.solrudev.ackpine.installer.InstallFailure
import ru.solrudev.ackpine.installer.InstallFailure.Timeout
import ru.solrudev.ackpine.installer.parameters.InstallConstraints
import ru.solrudev.ackpine.installer.parameters.InstallConstraints.TimeoutStrategy
import ru.solrudev.ackpine.installer.parameters.InstallMode
import ru.solrudev.ackpine.installer.parameters.InstallPreapproval
import ru.solrudev.ackpine.installer.parameters.PackageSource
import ru.solrudev.ackpine.session.Progress
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.session.Session.State.Completed
import ru.solrudev.ackpine.session.Session.State.Failed
import ru.solrudev.ackpine.session.parameters.Confirmation
import ru.solrudev.ackpine.session.parameters.NotificationData
import java.util.UUID
import java.util.concurrent.CancellationException
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random
import kotlin.random.nextInt

private const val TAG = "SessionBasedInstallSession"

@RestrictTo(RestrictTo.Scope.LIBRARY)
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
internal class SessionBasedInstallSession internal constructor(
	private val context: Context,
	private val packageInstaller: PackageInstallerService,
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
	handler: Handler,
	private val sessionCallbackHandler: Handler,
	@Volatile private var nativeSessionId: Int,
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
	private var sessionCallback = if (nativeSessionId != -1) {
		try {
			packageInstaller.createAndRegisterSessionCallback(nativeSessionId)
		} catch (exception: Exception) {
			completeExceptionally(exception)
			null
		}
	} else {
		null
	}

	@Volatile
	private var isPreapprovalActive = false

	@Volatile
	private var ignorePreapproval = false

	private val commitAttempts = AtomicInteger(commitAttemptsCount)

	override fun prepare() {
		if (isPreapprovalActive) {
			return
		}
		val sessionId = getSessionId()
		if (!shouldRequestPreapproval()) {
			writeApksToSession(sessionId)
			return
		}
		val preapprovalDetails = createPackageInstallerPreapprovalDetails()
		packageInstaller.openSession(sessionId).requestUserPreapproval(
			preapprovalDetails,
			createPackageInstallerStatusIntentSender()
		)
	}

	override fun launchConfirmation() {
		if (!shouldApplyInstallConstraints() || shouldCommitNormallyAfterTimeout()) {
			commitPackageInstallerSession()
		} else try {
			commitPackageInstallerSessionWithConstraints()
		} catch (exception: SecurityException) {
			Log.w(TAG, "$id: ${exception.message}")
			commitPackageInstallerSession()
		}
		val currentAttempt = commitAttempts.incrementAndGet()
		notifyCommitted()
		dbWriteSemaphore.withPermit {
			installConstraintsDao.setCommitAttemptsCount(id.toString(), currentAttempt)
		}
	}

	override fun onPreapprovalStarted() {
		isPreapprovalActive = true
	}

	override fun onPreapprovalSucceeded() {
		isPreapproved = true
		isPreapprovalActive = false
		executor.execute {
			dbWriteSemaphore.withPermit {
				installPreapprovalDao.setPreapproved(id.toString())
			}
		}
		executor.execute(::prepare)
	}

	override fun onPreapprovalFailed(
		status: PackageInstallerStatus?,
		publicFailure: InstallFailure
	) {
		isPreapprovalActive = false
		if (
			preapproval.fallbackToOnDemandApproval
			&& status == PackageInstallerStatus.INSTALL_FAILED_PRE_APPROVAL_NOT_AVAILABLE
		) {
			ignorePreapproval = true
			nativeSessionId = -1
			executor.execute {
				dbWriteSemaphore.withPermit {
					nativeSessionIdDao.removeNativeSessionId(id.toString())
				}
				prepare()
			}
			return
		}
		complete(Failed(publicFailure))
	}

	override fun onCompleted(state: Completed<InstallFailure>): Boolean {
		if (!shouldApplyInstallConstraints() || constraints.timeoutStrategy == TimeoutStrategy.Fail) {
			return true
		}
		if (state !is Failed || state.failure !is Timeout) {
			return true
		}
		if (shouldCommitNormallyAfterTimeout()) {
			notifyAwaiting()
			return false
		}
		val timeoutStrategy = constraints.timeoutStrategy
		if (timeoutStrategy is TimeoutStrategy.Retry) {
			val currentAttempt = commitAttempts.get()
			val shouldRetry = currentAttempt <= timeoutStrategy.retries
			if (shouldRetry) {
				Log.i(TAG, "Retrying $id: attempt #$currentAttempt")
				notifyAwaiting()
			}
			return !shouldRetry
		}
		return true
	}

	override fun doCleanup() {
		isPreapprovalActive = false
		clearPackageInstallerSessionCallback()
		executor.execute(::abandonSession) // may be long if storage is under load
	}

	@ChecksSdkIntAtLeast(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
	private fun shouldRequestPreapproval(): Boolean {
		return !isPreapproved
				&& !ignorePreapproval
				&& preapproval != InstallPreapproval.NONE
				&& Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
	}

	@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
	private fun createPackageInstallerPreapprovalDetails(): PackageInstaller.PreapprovalDetails {
		val icon = readPreapprovalIconBitmap()
		val builder = PackageInstaller.PreapprovalDetails.Builder()
			.setPackageName(preapproval.packageName)
			.setLabel(preapproval.label)
			.setLocale(ULocale.forLanguageTag(preapproval.languageTag))
		if (icon != null) {
			builder.setIcon(icon)
		}
		return builder.build()
	}

	private fun readPreapprovalIconBitmap(): Bitmap? {
		if (preapproval.icon == Uri.EMPTY) {
			return null
		}
		return try {
			context.contentResolver.openInputStream(preapproval.icon)?.use { iconStream ->
				iconStream.buffered().use { bufferedIconStream ->
					BitmapFactory.decodeStream(bufferedIconStream)
				}
			}
		} catch (_: Exception) {
			null
		}
	}

	@ChecksSdkIntAtLeast(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
	private fun shouldApplyInstallConstraints(): Boolean {
		return constraints != InstallConstraints.NONE
				&& Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
	}

	private fun shouldCommitNormallyAfterTimeout(): Boolean {
		return constraints.timeoutStrategy == TimeoutStrategy.CommitEagerly
				&& commitAttempts.get() == 1
	}

	private fun commitPackageInstallerSession() {
		val statusReceiver = createPackageInstallerStatusIntentSender()
		val sessionId = nativeSessionId
		if (packageInstaller.getSessionInfo(sessionId) != null) {
			writeCommitProgressIfAbsent()
			packageInstaller.openSession(sessionId).commit(statusReceiver)
		}
	}

	@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
	private fun commitPackageInstallerSessionWithConstraints() {
		val statusReceiver = createPackageInstallerStatusIntentSender()
		val sessionId = nativeSessionId
		if (packageInstaller.getSessionInfo(sessionId) != null) {
			val installConstraints = createPackageInstallerInstallConstraints()
			writeCommitProgressIfAbsent()
			packageInstaller.commitSessionAfterInstallConstraintsAreMet(
				sessionId, statusReceiver, installConstraints, constraints.timeoutMillis
			)
		}
	}

	private fun createPackageInstallerStatusIntentSender(): IntentSender {
		return createPackageInstallerStatusIntentSender<PackageInstallerStatusReceiver>(
			context,
			action = PackageInstallerStatusReceiver.getAction(context),
			sessionId = id,
			confirmation, notificationId, notificationData, generateRequestCode()
		) { intent ->
			intent.putExtra(SystemPackageInstallerStatusReceiver.EXTRA_REQUIRE_USER_ACTION, requireUserAction)
		}
	}

	@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
	private fun createPackageInstallerInstallConstraints(): PackageInstaller.InstallConstraints {
		val builder = PackageInstaller.InstallConstraints.Builder()
		if (constraints.isAppNotForegroundRequired) {
			builder.setAppNotForegroundRequired()
		}
		if (constraints.isAppNotInteractingRequired) {
			builder.setAppNotInteractingRequired()
		}
		if (constraints.isAppNotTopVisibleRequired) {
			builder.setAppNotTopVisibleRequired()
		}
		if (constraints.isDeviceIdleRequired) {
			builder.setDeviceIdleRequired()
		}
		if (constraints.isNotInCallRequired) {
			builder.setNotInCallRequired()
		}
		return builder.build()
	}

	private fun writeCommitProgressIfAbsent() = CommitProgressValueHolder.putIfAbsent(context) {
		packageInstaller.getSessionInfo(nativeSessionId)!!.progress + 0.01f
	}

	private fun getSessionId(): Int {
		val sessionId = nativeSessionId
		if (sessionId != -1) {
			return sessionId
		}
		return createSession()
	}

	private fun createSession(): Int {
		val sessionId = packageInstaller.createSession(createSessionParams(), id)
		nativeSessionId = sessionId
		persistNativeSessionId(sessionId)
		clearPackageInstallerSessionCallback()
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
			sessionParams.setOriginatingUid(packageInstaller.uid)
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
		session.writeApksAsync().handleResult(
			block = {
				try {
					session.close()
					notifyAwaiting()
				} catch (exception: Exception) {
					completeExceptionally(exception)
				}
			},
			onException = { exception ->
				session.closeWithException(exception)
				if (exception !is CancellationException) {
					completeExceptionally(exception)
				}
			}
		)
	}

	private fun PackageInstallerService.Session.writeApksAsync() = CallbackToFutureAdapter.getFuture { completer ->
		writeApks(completer)
	}

	private fun PackageInstallerService.Session.writeApks(
		completer: CallbackToFutureAdapter.Completer<Unit>
	): String {
		val tag = "SessionBasedInstallSession.writeApks"
		val assetFileDescriptors = apks
			.mapCatchingFirst { uri ->
				context.openAssetFileDescriptor(uri, cancellationSignal)
					?: throw NullPointerException("AssetFileDescriptor was null: $uri")
			}
			.getOrElse { failure ->
				closeAllWithException(failure.partialResult, failure.exception)
				if (failure.exception is OperationCanceledException) {
					completer.setCancelled()
				} else {
					completer.setException(failure.exception)
				}
				return tag
			}
		val countdown = AtomicInteger(apks.size)
		val currentProgress = AtomicInteger(0)
		val progressMax = apks.size * PROGRESS_MAX
		val sharedCancelSignal = CancellationSignal()
		cancellationSignal.setOnCancelListener {
			sharedCancelSignal.cancel()
			completer.setCancelled()
		}
		assetFileDescriptors.forEachIndexed { index, afd ->
			try {
				executor.execute {
					try {
						afd.use {
							writeApk(afd, index, currentProgress, progressMax, sharedCancelSignal)
							if (countdown.decrementAndGet() == 0) {
								completer.set(Unit)
							}
						}
					} catch (_: OperationCanceledException) { // no-op
					} catch (throwable: Throwable) {
						sharedCancelSignal.cancel()
						completer.setException(throwable)
					}
				}
			} catch (exception: Exception) {
				afd.closeWithException(exception)
				sharedCancelSignal.cancel()
				completer.setException(exception)
			}
		}
		return tag
	}

	private fun PackageInstallerService.Session.writeApk(
		afd: AssetFileDescriptor,
		index: Int,
		currentProgress: AtomicInteger,
		progressMax: Int,
		cancellationSignal: CancellationSignal
	) = afd.createInputStream().use { apkStream ->
		checkNotNull(apkStream) { "APK $index InputStream was null." }
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

	private fun PackageInstallerService.createAndRegisterSessionCallback(
		nativeSessionId: Int
	): PackageInstaller.SessionCallback {
		val callback = packageInstallerSessionCallback(nativeSessionId)
		registerSessionCallback(callback, sessionCallbackHandler)
		return callback
	}

	private fun clearPackageInstallerSessionCallback() {
		val callback = sessionCallback ?: return
		sessionCallback = null
		try {
			packageInstaller.unregisterSessionCallback(callback)
		} catch (_: Throwable) { // no-op
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

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun PackageSource.toPackageInstallerPackageSource() = when (this) {
	PackageSource.Unspecified -> PackageInstaller.PACKAGE_SOURCE_UNSPECIFIED
	PackageSource.Store -> PackageInstaller.PACKAGE_SOURCE_STORE
	PackageSource.LocalFile -> PackageInstaller.PACKAGE_SOURCE_LOCAL_FILE
	PackageSource.DownloadedFile -> PackageInstaller.PACKAGE_SOURCE_DOWNLOADED_FILE
	PackageSource.Other -> PackageInstaller.PACKAGE_SOURCE_OTHER
	else -> PackageInstaller.PACKAGE_SOURCE_UNSPECIFIED
}