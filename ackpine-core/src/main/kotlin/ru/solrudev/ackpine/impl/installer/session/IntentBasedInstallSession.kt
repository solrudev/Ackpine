package ru.solrudev.ackpine.impl.installer.session

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import android.os.Environment
import android.os.Handler
import androidx.annotation.RestrictTo
import androidx.core.net.toUri
import ru.solrudev.ackpine.impl.database.dao.NotificationIdDao
import ru.solrudev.ackpine.impl.database.dao.SessionDao
import ru.solrudev.ackpine.impl.database.dao.SessionFailureDao
import ru.solrudev.ackpine.impl.database.dao.SessionProgressDao
import ru.solrudev.ackpine.impl.installer.activity.IntentBasedInstallActivity
import ru.solrudev.ackpine.impl.installer.session.helpers.STREAM_COPY_PROGRESS_MAX
import ru.solrudev.ackpine.impl.installer.session.helpers.copyTo
import ru.solrudev.ackpine.impl.installer.session.helpers.openAssetFileDescriptor
import ru.solrudev.ackpine.impl.installer.session.helpers.toFile
import ru.solrudev.ackpine.impl.session.AbstractProgressSession
import ru.solrudev.ackpine.impl.session.helpers.CANCEL_CURRENT_FLAGS
import ru.solrudev.ackpine.impl.session.helpers.launchConfirmation
import ru.solrudev.ackpine.installer.InstallFailure
import ru.solrudev.ackpine.session.Progress
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.session.parameters.Confirmation
import ru.solrudev.ackpine.session.parameters.NotificationData
import java.io.File
import java.util.UUID
import java.util.concurrent.Executor

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class IntentBasedInstallSession internal constructor(
	private val context: Context,
	private val apk: Uri,
	id: UUID,
	initialState: Session.State<InstallFailure>,
	initialProgress: Progress,
	private val confirmation: Confirmation,
	private val notificationData: NotificationData,
	sessionDao: SessionDao,
	sessionFailureDao: SessionFailureDao<InstallFailure>,
	sessionProgressDao: SessionProgressDao,
	notificationIdDao: NotificationIdDao,
	serialExecutor: Executor,
	handler: Handler
) : AbstractProgressSession<InstallFailure>(
	context, INSTALLER_NOTIFICATION_TAG,
	id, initialState, initialProgress,
	sessionDao, sessionFailureDao, sessionProgressDao, notificationIdDao,
	serialExecutor, handler,
	exceptionalFailureFactory = InstallFailure::Exceptional
) {

	private val Context.externalDir: File
		get() {
			val externalFilesDir = getExternalFilesDir(null)
			return if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED && externalFilesDir != null) {
				externalFilesDir
			} else {
				filesDir
			}
		}

	private val copyFile = File(context.externalDir, "ackpine/sessions/$id/0.apk")

	override fun doLaunch(cancellationSignal: CancellationSignal) {
		val (_, mustCopy) = getApkUri(cancellationSignal)
		if (mustCopy) {
			createApkCopy(cancellationSignal)
		}
		notifyAwaiting()
	}

	override fun doCommit(cancellationSignal: CancellationSignal) {
		val (apkUri, _) = getApkUri(cancellationSignal)
		context.launchConfirmation<IntentBasedInstallActivity>(
			confirmation, notificationData,
			sessionId = id,
			INSTALLER_NOTIFICATION_TAG, notificationId,
			INSTALLER_REQUEST_CODE,
			CANCEL_CURRENT_FLAGS
		) { intent -> intent.putExtra(IntentBasedInstallActivity.APK_URI_KEY, apkUri) }
	}

	override fun doCleanup() {
		copyFile.delete()
	}

	private fun getApkUri(cancellationSignal: CancellationSignal): ApkUri {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
			val file = apk.toFile(context, cancellationSignal)
			if (file.canRead()) {
				return ApkUri(file.toUri(), mustCopy = false)
			}
			return ApkUri(copyFile.toUri(), mustCopy = true)
		}
		return ApkUri(apk, mustCopy = false)
	}

	private fun createApkCopy(cancellationSignal: CancellationSignal) {
		if (copyFile.exists()) {
			copyFile.delete()
		}
		copyFile.createNewFile()
		val afd = context.openAssetFileDescriptor(apk, cancellationSignal)
			?: error("AssetFileDescriptor was null: $apk")
		afd.createInputStream().buffered().use { apkStream ->
			copyFile.outputStream().buffered().use { outputStream ->
				var currentProgress = 0
				apkStream.copyTo(outputStream, afd.declaredLength, cancellationSignal, onProgress = { delta ->
					currentProgress += delta
					progress = Progress(currentProgress, STREAM_COPY_PROGRESS_MAX)
				})
			}
		}
	}

	private data class ApkUri(val uri: Uri, val mustCopy: Boolean)
}