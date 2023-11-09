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

package ru.solrudev.ackpine.impl.installer.session

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import android.os.Environment
import android.os.Handler
import androidx.annotation.RestrictTo
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import androidx.core.net.toUri
import ru.solrudev.ackpine.AckpineFileProvider
import ru.solrudev.ackpine.helpers.toFile
import ru.solrudev.ackpine.impl.database.dao.NotificationIdDao
import ru.solrudev.ackpine.impl.database.dao.SessionDao
import ru.solrudev.ackpine.impl.database.dao.SessionFailureDao
import ru.solrudev.ackpine.impl.database.dao.SessionProgressDao
import ru.solrudev.ackpine.impl.installer.activity.IntentBasedInstallActivity
import ru.solrudev.ackpine.impl.installer.session.helpers.STREAM_COPY_PROGRESS_MAX
import ru.solrudev.ackpine.impl.installer.session.helpers.copyTo
import ru.solrudev.ackpine.impl.installer.session.helpers.openAssetFileDescriptor
import ru.solrudev.ackpine.impl.session.AbstractProgressSession
import ru.solrudev.ackpine.impl.session.globalNotificationId
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
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.random.nextInt

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
	handler: Handler,
	newNotificationId: Int = globalNotificationId.incrementAndGet()
) : AbstractProgressSession<InstallFailure>(
	context, INSTALLER_NOTIFICATION_TAG,
	id, initialState, initialProgress,
	sessionDao, sessionFailureDao, sessionProgressDao, notificationIdDao,
	serialExecutor, handler,
	exceptionalFailureFactory = InstallFailure::Exceptional,
	newNotificationId
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

	override fun prepare(cancellationSignal: CancellationSignal) {
		val (_, mustCopy) = getApkUri(cancellationSignal)
		if (mustCopy) {
			createApkCopy(cancellationSignal)
		}
		notifyAwaiting()
	}

	override fun launchConfirmation(cancellationSignal: CancellationSignal, notificationId: Int) {
		val (apkUri, _) = getApkUri(cancellationSignal)
		context.launchConfirmation<IntentBasedInstallActivity>(
			confirmation, notificationData,
			sessionId = id,
			INSTALLER_NOTIFICATION_TAG, notificationId,
			generateRequestCode(),
			CANCEL_CURRENT_FLAGS
		) { intent -> intent.putExtra(IntentBasedInstallActivity.APK_URI_KEY, apkUri) }
	}

	override fun doCleanup() {
		copyFile.delete()
	}

	override fun onCommitted() {
		progress = Progress((STREAM_COPY_PROGRESS_MAX * 0.9).roundToInt(), STREAM_COPY_PROGRESS_MAX)
	}

	override fun onCompleted(success: Boolean) {
		if (success) {
			progress = Progress(STREAM_COPY_PROGRESS_MAX, STREAM_COPY_PROGRESS_MAX)
		}
	}

	private fun getApkUri(cancellationSignal: CancellationSignal): ApkUri {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
			val file = apk.toFile(context, cancellationSignal)
			if (file.canRead()) {
				return ApkUri(file.toUri(), mustCopy = false)
			}
			return ApkUri(copyFile.toUri(), mustCopy = true)
		}
		if (apk.scheme == ContentResolver.SCHEME_FILE) {
			return ApkUri(
				FileProvider.getUriForFile(context, AckpineFileProvider.authority, apk.toFile()),
				mustCopy = false
			)
		}
		return ApkUri(apk, mustCopy = false)
	}

	private fun createApkCopy(cancellationSignal: CancellationSignal) {
		if (copyFile.exists()) {
			copyFile.delete()
		}
		copyFile.parentFile?.mkdirs()
		copyFile.createNewFile()
		val afd = context.openAssetFileDescriptor(apk, cancellationSignal)
			?: error("AssetFileDescriptor was null: $apk")
		afd.createInputStream().buffered().use { apkStream ->
			val outputStream = copyFile.outputStream()
			outputStream.buffered().use { bufferedOutputStream ->
				var currentProgress = 0
				apkStream.copyTo(bufferedOutputStream, afd.declaredLength, cancellationSignal, onProgress = { delta ->
					currentProgress += delta
					progress = Progress((currentProgress * 0.8).roundToInt(), STREAM_COPY_PROGRESS_MAX)
				})
				bufferedOutputStream.flush()
				outputStream.fd.sync()
			}
		}
	}

	private fun generateRequestCode() = Random.nextInt(2000000..3000000)

	private data class ApkUri(val uri: Uri, val mustCopy: Boolean)
}