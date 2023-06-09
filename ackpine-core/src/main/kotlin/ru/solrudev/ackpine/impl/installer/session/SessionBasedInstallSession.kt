package ru.solrudev.ackpine.impl.installer.session

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import android.os.Handler
import android.os.OperationCanceledException
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.concurrent.futures.ResolvableFuture
import com.google.common.util.concurrent.ListenableFuture
import ru.solrudev.ackpine.helpers.handleResult
import ru.solrudev.ackpine.impl.database.dao.NativeSessionIdDao
import ru.solrudev.ackpine.impl.database.dao.NotificationIdDao
import ru.solrudev.ackpine.impl.database.dao.SessionDao
import ru.solrudev.ackpine.impl.database.dao.SessionFailureDao
import ru.solrudev.ackpine.impl.database.dao.SessionProgressDao
import ru.solrudev.ackpine.impl.installer.activity.SessionBasedInstallCommitActivity
import ru.solrudev.ackpine.impl.installer.session.helpers.STREAM_COPY_PROGRESS_MAX
import ru.solrudev.ackpine.impl.installer.session.helpers.copyTo
import ru.solrudev.ackpine.impl.installer.session.helpers.openAssetFileDescriptor
import ru.solrudev.ackpine.impl.session.AbstractProgressSession
import ru.solrudev.ackpine.impl.session.helpers.CANCEL_CURRENT_FLAGS
import ru.solrudev.ackpine.impl.session.helpers.launchConfirmation
import ru.solrudev.ackpine.installer.InstallFailure
import ru.solrudev.ackpine.session.Progress
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.session.parameters.Confirmation
import ru.solrudev.ackpine.session.parameters.NotificationData
import java.util.UUID
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

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
	sessionDao: SessionDao,
	sessionFailureDao: SessionFailureDao<InstallFailure>,
	sessionProgressDao: SessionProgressDao,
	private val nativeSessionIdDao: NativeSessionIdDao,
	notificationIdDao: NotificationIdDao,
	private val executor: Executor,
	serialExecutor: Executor,
	private val handler: Handler,
) : AbstractProgressSession<InstallFailure>(
	context, INSTALLER_NOTIFICATION_TAG,
	id, initialState, initialProgress,
	sessionDao, sessionFailureDao, sessionProgressDao, notificationIdDao,
	serialExecutor, handler,
	exceptionalFailureFactory = InstallFailure::Exceptional
) {

	init {
		serialExecutor.execute {
			nativeSessionId = nativeSessionIdDao.getNativeSessionId(id.toString()) ?: -1
		}
	}

	@Volatile
	private var nativeSessionId = -1

	private val packageInstaller: PackageInstaller
		get() = context.packageManager.packageInstaller

	private var sessionCallback: PackageInstaller.SessionCallback? = null

	override fun prepare(cancellationSignal: CancellationSignal) {
		if (nativeSessionId != -1) {
			abandonSession()
		}
		val sessionParams = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			sessionParams.setInstallReason(PackageManager.INSTALL_REASON_USER)
		}
		val sessionId = packageInstaller.createSession(sessionParams)
		nativeSessionId = sessionId
		persistNativeSessionId(sessionId)
		sessionCallback = packageInstaller.createAndRegisterSessionCallback()
		packageInstaller.openSession(sessionId).use { session ->
			session.writeApks(cancellationSignal).handleResult(
				onException = { exception ->
					if (exception is OperationCanceledException) {
						cancel()
					} else {
						completeExceptionally(exception)
					}
				},
				block = { notifyAwaiting() })
		}
	}

	override fun launchConfirmation(cancellationSignal: CancellationSignal, notificationId: Int) {
		context.launchConfirmation<SessionBasedInstallCommitActivity>(
			confirmation, notificationData,
			sessionId = id,
			INSTALLER_NOTIFICATION_TAG, notificationId,
			INSTALLER_REQUEST_CODE,
			CANCEL_CURRENT_FLAGS
		) { intent -> intent.putExtra(PackageInstaller.EXTRA_SESSION_ID, nativeSessionId) }
	}

	override fun doCleanup() {
		abandonSession()
		handler.post {
			sessionCallback?.let(packageInstaller::unregisterSessionCallback)
		}
	}

	@SuppressLint("RestrictedApi")
	private fun PackageInstaller.Session.writeApks(cancellationSignal: CancellationSignal): ListenableFuture<Unit> {
		val future = ResolvableFuture.create<Unit>()
		val isThrown = AtomicBoolean(false)
		val countdown = AtomicInteger(apks.size)
		val currentProgress = AtomicInteger(0)
		val progressMax = apks.size * STREAM_COPY_PROGRESS_MAX
		apks.forEachIndexed { index, uri ->
			try {
				executor.execute {
					try {
						writeApk(index, uri, currentProgress, progressMax, isThrown, cancellationSignal)
						if (countdown.decrementAndGet() == 0) {
							future.set(Unit)
						}
					} catch (t: Throwable) {
						isThrown.set(true)
						future.setException(t)
					}
				}
			} catch (t: Throwable) {
				isThrown.set(true)
				future.setException(t)
			}
		}
		return future
	}

	private fun PackageInstaller.Session.writeApk(
		index: Int,
		uri: Uri,
		currentProgress: AtomicInteger,
		progressMax: Int,
		isThrown: AtomicBoolean,
		cancellationSignal: CancellationSignal
	) {
		val afd = context.openAssetFileDescriptor(uri, cancellationSignal)
			?: error("AssetFileDescriptor was null: $uri")
		val length = afd.declaredLength
		afd.createInputStream().use { apkStream ->
			requireNotNull(apkStream) { "APK $index InputStream was null." }
			openWrite("$index.apk", 0, length).buffered().use { sessionStream ->
				apkStream.copyTo(sessionStream, length, cancellationSignal, onProgress = { progress ->
					if (isThrown.get()) {
						return
					}
					val current = currentProgress.addAndGet(progress)
					setStagingProgress(current.toFloat() / progressMax)
				})
			}
		}
	}

	private fun PackageInstaller.createAndRegisterSessionCallback(): PackageInstaller.SessionCallback {
		val callback = packageInstallerSessionCallback()
		handler.post {
			registerSessionCallback(callback)
		}
		return callback
	}

	private fun packageInstallerSessionCallback() = object : PackageInstaller.SessionCallback() {
		override fun onCreated(sessionId: Int) {}
		override fun onBadgingChanged(sessionId: Int) {}
		override fun onActiveChanged(sessionId: Int, active: Boolean) {}
		override fun onFinished(sessionId: Int, success: Boolean) {}

		override fun onProgressChanged(sessionId: Int, progress: Float) {
			if (sessionId == nativeSessionId) {
				this@SessionBasedInstallSession.progress = Progress((progress * 100).toInt(), 100)
			}
		}
	}

	private fun abandonSession() {
		try {
			context.packageManager.packageInstaller.abandonSession(nativeSessionId)
		} catch (_: Throwable) {
		}
	}

	private fun persistNativeSessionId(nativeSessionId: Int) {
		nativeSessionIdDao.setNativeSessionId(id.toString(), nativeSessionId)
	}
}