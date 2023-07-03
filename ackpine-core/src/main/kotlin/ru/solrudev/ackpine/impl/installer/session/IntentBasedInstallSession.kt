package ru.solrudev.ackpine.impl.installer.session

import android.content.Context
import android.net.Uri
import android.os.CancellationSignal
import android.os.Handler
import androidx.annotation.RestrictTo
import androidx.core.net.toUri
import ru.solrudev.ackpine.impl.database.dao.SessionDao
import ru.solrudev.ackpine.impl.database.dao.SessionFailureDao
import ru.solrudev.ackpine.impl.database.dao.SessionProgressDao
import ru.solrudev.ackpine.impl.installer.activity.InstallActivity
import ru.solrudev.ackpine.impl.installer.activity.IntentBasedInstallActivity
import ru.solrudev.ackpine.impl.installer.session.helpers.CANCEL_CURRENT_FLAGS
import ru.solrudev.ackpine.impl.installer.session.helpers.INSTALLER_NOTIFICATION_TAG
import ru.solrudev.ackpine.impl.installer.session.helpers.INSTALLER_REQUEST_CODE
import ru.solrudev.ackpine.impl.installer.session.helpers.STREAM_COPY_PROGRESS_MAX
import ru.solrudev.ackpine.impl.installer.session.helpers.copyTo
import ru.solrudev.ackpine.impl.installer.session.helpers.launchConfirmation
import ru.solrudev.ackpine.impl.installer.session.helpers.openAssetFileDescriptor
import ru.solrudev.ackpine.impl.installer.session.helpers.toFile
import ru.solrudev.ackpine.impl.session.AbstractProgressSession
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
	executor: Executor,
	handler: Handler
) : AbstractProgressSession<InstallFailure>(
	id, initialState, initialProgress, sessionDao, sessionFailureDao, sessionProgressDao, executor, handler,
	exceptionalFailureFactory = InstallFailure::Exceptional
) {

	private val copyFile = File(context.filesDir, "ackpine/sessions/$id/0.apk")
	private val cancellationSignal = CancellationSignal()

	override fun doLaunch() {
		val (apkFile, mustCopy) = getApkFile()
		if (mustCopy) {
			copyApkTo(apkFile)
		}
		notifyAwaiting()
	}

	override fun doCommit() {
		val (apkFile, _) = getApkFile()
		context.launchConfirmation<IntentBasedInstallActivity>(
			confirmation,
			notificationData,
			INSTALLER_NOTIFICATION_TAG,
			INSTALLER_REQUEST_CODE,
			CANCEL_CURRENT_FLAGS
		) { intent ->
			intent.run {
				putExtra(InstallActivity.SESSION_ID_KEY, id)
				putExtra(IntentBasedInstallActivity.APK_URI_KEY, apkFile.toUri())
			}
		}
	}

	override fun doCancel() {
		cancellationSignal.cancel()
	}

	override fun cleanup() {
		copyFile.delete()
	}

	private fun getApkFile(): ApkFile {
		val file = apk.toFile(context, cancellationSignal)
		if (file.canRead()) {
			return ApkFile(file, mustCopy = false)
		}
		return ApkFile(copyFile, mustCopy = true)
	}

	private fun copyApkTo(apkFile: File) {
		if (apkFile.exists()) {
			apkFile.delete()
		}
		apkFile.createNewFile()
		val afd = context.openAssetFileDescriptor(apk, cancellationSignal)
			?: error("AssetFileDescriptor was null: $apk")
		afd.createInputStream().buffered().use { apkStream ->
			apkFile.outputStream().buffered().use { outputStream ->
				var currentProgress = 0
				apkStream.copyTo(outputStream, afd.declaredLength, cancellationSignal, onProgress = { delta ->
					currentProgress += delta
					progress = Progress(currentProgress, STREAM_COPY_PROGRESS_MAX)
				})
			}
		}
	}

	private data class ApkFile(val file: File, val mustCopy: Boolean)
}