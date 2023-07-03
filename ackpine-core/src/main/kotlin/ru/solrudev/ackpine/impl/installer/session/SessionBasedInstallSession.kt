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
import ru.solrudev.ackpine.impl.database.dao.SessionDao
import ru.solrudev.ackpine.impl.database.dao.SessionFailureDao
import ru.solrudev.ackpine.impl.database.dao.SessionProgressDao
import ru.solrudev.ackpine.impl.installer.activity.InstallActivity
import ru.solrudev.ackpine.impl.installer.activity.SessionBasedInstallLauncherActivity
import ru.solrudev.ackpine.impl.installer.session.helpers.CANCEL_CURRENT_FLAGS
import ru.solrudev.ackpine.impl.installer.session.helpers.INSTALLER_NOTIFICATION_TAG
import ru.solrudev.ackpine.impl.installer.session.helpers.INSTALLER_REQUEST_CODE
import ru.solrudev.ackpine.impl.installer.session.helpers.STREAM_COPY_PROGRESS_MAX
import ru.solrudev.ackpine.impl.installer.session.helpers.copyTo
import ru.solrudev.ackpine.impl.installer.session.helpers.launchConfirmation
import ru.solrudev.ackpine.impl.installer.session.helpers.openAssetFileDescriptor
import ru.solrudev.ackpine.impl.session.AbstractProgressSession
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
	private val executor: Executor,
	private val handler: Handler,
) : AbstractProgressSession<InstallFailure>(
	id, initialState, initialProgress, sessionDao, sessionFailureDao, sessionProgressDao, executor, handler,
	exceptionalFailureFactory = InstallFailure::Exceptional
) {

	@Volatile
	private var nativeSessionId = -1

	private val packageInstaller: PackageInstaller
		get() = context.packageManager.packageInstaller

	private val cancellationSignal = CancellationSignal()
	private var sessionCallback: PackageInstaller.SessionCallback? = null

	init {
		executor.execute {
			nativeSessionId = nativeSessionIdDao.getNativeSessionId(id.toString()) ?: -1
		}
	}

	override fun doLaunch() {
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
			session.writeApks().handleResult {
				notifyAwaiting()
			}
		}
	}

	@SuppressLint("RestrictedApi")
	private fun PackageInstaller.Session.writeApks(): ListenableFuture<Unit> {
		val future = ResolvableFuture.create<Unit>()
		val isThrown = AtomicBoolean(false)
		val countdown = AtomicInteger(apks.size)
		val currentProgress = AtomicInteger(0)
		val max = apks.size * STREAM_COPY_PROGRESS_MAX
		apks.forEachIndexed { index, uri ->
			try {
				executor.execute {
					try {
						val afd = context.openAssetFileDescriptor(uri, cancellationSignal)
							?: error("AssetFileDescriptor was null: $uri")
						val length = afd.declaredLength
						afd.createInputStream().use { apkStream ->
							requireNotNull(apkStream) { "APK $index InputStream was null." }
							openWrite("$index.apk", 0, length).buffered().use { sessionStream ->
								apkStream.copyTo(sessionStream, length, cancellationSignal, onProgress = { progress ->
									if (isThrown.get()) {
										throw OperationCanceledException()
									}
									val current = currentProgress.addAndGet(progress)
									setStagingProgress(current.toFloat() / max)
								})
							}
						}
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

	override fun doCommit() {
		context.launchConfirmation<SessionBasedInstallLauncherActivity>(
			confirmation,
			notificationData,
			INSTALLER_NOTIFICATION_TAG,
			INSTALLER_REQUEST_CODE,
			CANCEL_CURRENT_FLAGS
		) { intent ->
			intent.run {
				putExtra(InstallActivity.SESSION_ID_KEY, id)
				putExtra(PackageInstaller.EXTRA_SESSION_ID, nativeSessionId)
			}
		}
	}

	override fun doCancel() {
		cancellationSignal.cancel()
	}

	override fun cleanup() {
		abandonSession()
		sessionCallback?.let(packageInstaller::unregisterSessionCallback)
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