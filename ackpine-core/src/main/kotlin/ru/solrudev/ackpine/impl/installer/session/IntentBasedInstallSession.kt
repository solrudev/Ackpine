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

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_DENIED
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import androidx.annotation.RestrictTo
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import ru.solrudev.ackpine.AckpineFileProvider
import ru.solrudev.ackpine.impl.database.dao.LastUpdateTimestampDao
import ru.solrudev.ackpine.impl.database.dao.SessionDao
import ru.solrudev.ackpine.impl.database.dao.SessionFailureDao
import ru.solrudev.ackpine.impl.database.dao.SessionProgressDao
import ru.solrudev.ackpine.impl.helpers.CANCEL_CURRENT_FLAGS
import ru.solrudev.ackpine.impl.helpers.concurrent.BinarySemaphore
import ru.solrudev.ackpine.impl.helpers.concurrent.withPermit
import ru.solrudev.ackpine.impl.helpers.launchConfirmation
import ru.solrudev.ackpine.impl.installer.activity.IntentBasedInstallActivity
import ru.solrudev.ackpine.impl.installer.session.helpers.PROGRESS_MAX
import ru.solrudev.ackpine.impl.installer.session.helpers.copyTo
import ru.solrudev.ackpine.impl.installer.session.helpers.openAssetFileDescriptor
import ru.solrudev.ackpine.impl.session.AbstractProgressSession
import ru.solrudev.ackpine.installer.InstallFailure
import ru.solrudev.ackpine.session.Progress
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.session.Session.State.Completed
import ru.solrudev.ackpine.session.Session.State.Succeeded
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
	private val lastUpdateTimestampDao: LastUpdateTimestampDao,
	sessionDao: SessionDao,
	sessionFailureDao: SessionFailureDao<InstallFailure>,
	sessionProgressDao: SessionProgressDao,
	private val executor: Executor,
	handler: Handler,
	notificationId: Int,
	private val dbWriteSemaphore: BinarySemaphore
) : AbstractProgressSession<InstallFailure>(
	context, id, initialState, initialProgress,
	sessionDao, sessionFailureDao, sessionProgressDao,
	executor, handler,
	exceptionalFailureFactory = InstallFailure::Exceptional,
	notificationId, dbWriteSemaphore
) {

	@Volatile
	private var apkFile: File? = null

	override fun prepare() {
		createApkCopy()
		val apkPackageName = context.packageManager
			.getPackageArchiveInfo(getOrCreateApkFile().absolutePath, 0)
			?.packageName
			.orEmpty()
		if (context.packageName == apkPackageName) {
			dbWriteSemaphore.withPermit {
				lastUpdateTimestampDao.setLastUpdateTimestamp(
					id.toString(),
					apkPackageName,
					getLastSelfUpdateTimestamp()
				)
			}
		}
		notifyAwaiting()
	}

	override fun launchConfirmation() {
		context.launchConfirmation<IntentBasedInstallActivity>(
			confirmation, notificationData,
			sessionId = id,
			notificationId,
			generateRequestCode(),
			CANCEL_CURRENT_FLAGS
		) { intent -> intent.putExtra(IntentBasedInstallActivity.APK_URI_KEY, getApkUri()) }
	}

	override fun doCleanup() = executor.execute {
		val file = apkFile ?: getApkOrNull()
		file?.delete()
	}

	override fun onCommitted() {
		setProgress((PROGRESS_MAX * 0.9).roundToInt())
	}

	override fun onCompleted(state: Completed<InstallFailure>): Boolean {
		if (state is Succeeded) {
			setProgress(PROGRESS_MAX)
		}
		return true
	}

	private fun getApkUri(): Uri {
		val file = getOrCreateApkFile()
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
			return file.toUri()
		}
		return FileProvider.getUriForFile(context, AckpineFileProvider.authority, file)
	}

	private fun createApkCopy() {
		val file = getOrCreateApkFile()
		if (file.exists()) {
			file.delete()
		}
		file.parentFile?.mkdirs()
		file.createNewFile()
		val afd = context.openAssetFileDescriptor(apk, cancellationSignal)
			?: throw NullPointerException("AssetFileDescriptor was null: $apk")
		afd.createInputStream().buffered().use { apkStream ->
			val outputStream = file.outputStream()
			outputStream.buffered().use { bufferedOutputStream ->
				var currentProgress = 0
				apkStream.copyTo(bufferedOutputStream, afd.declaredLength, cancellationSignal, onProgress = { delta ->
					currentProgress += delta
					setProgress((currentProgress * 0.8).roundToInt())
				})
				bufferedOutputStream.flush()
				outputStream.fd.sync()
			}
		}
	}

	private fun getOrCreateApkFile(): File {
		apkFile?.let { return it }
		val file = getApkOrNull()
		if (file == null) {
			val cause = if (ContextCompat.checkSelfPermission(context, WRITE_EXTERNAL_STORAGE) == PERMISSION_DENIED) {
				" WRITE_EXTERNAL_STORAGE permission denied."
			} else {
				""
			}
			completeExceptionally(IllegalStateException("External storage is not available.$cause"))
			return File("")
		}
		apkFile = file
		return file
	}

	private fun getApkOrNull(): File? {
		val rootDir = getRootApkDirOrNull() ?: return null
		return File(rootDir, "ackpine/sessions/$id/0.apk")
	}

	private fun getRootApkDirOrNull(): File? {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			return context.filesDir
		}
		val externalFilesDir = context.getExternalFilesDir(null)
		if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED && externalFilesDir != null) {
			return externalFilesDir
		}
		return null
	}

	private fun getLastSelfUpdateTimestamp(): Long {
		return context.packageManager.getPackageInfo(context.packageName, 0).lastUpdateTime
	}

	private fun generateRequestCode() = Random.nextInt(2000000..3000000)
}