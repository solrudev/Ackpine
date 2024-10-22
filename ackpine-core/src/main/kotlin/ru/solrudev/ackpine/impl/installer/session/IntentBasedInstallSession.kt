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

package ru.solrudev.ackpine.impl.installer.session

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import android.os.Environment
import android.os.Handler
import androidx.annotation.RestrictTo
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import ru.solrudev.ackpine.AckpineFileProvider
import ru.solrudev.ackpine.helpers.concurrent.BinarySemaphore
import ru.solrudev.ackpine.helpers.concurrent.withPermit
import ru.solrudev.ackpine.impl.database.dao.LastUpdateTimestampDao
import ru.solrudev.ackpine.impl.database.dao.SessionDao
import ru.solrudev.ackpine.impl.database.dao.SessionFailureDao
import ru.solrudev.ackpine.impl.database.dao.SessionProgressDao
import ru.solrudev.ackpine.impl.installer.activity.IntentBasedInstallActivity
import ru.solrudev.ackpine.impl.installer.session.helpers.STREAM_COPY_PROGRESS_MAX
import ru.solrudev.ackpine.impl.installer.session.helpers.copyTo
import ru.solrudev.ackpine.impl.installer.session.helpers.openAssetFileDescriptor
import ru.solrudev.ackpine.impl.session.AbstractProgressSession
import ru.solrudev.ackpine.impl.session.helpers.CANCEL_CURRENT_FLAGS
import ru.solrudev.ackpine.impl.session.helpers.launchConfirmation
import ru.solrudev.ackpine.installer.InstallFailure
import ru.solrudev.ackpine.session.Progress
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.session.Session.State.Committed
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
	executor: Executor,
	handler: Handler,
	notificationId: Int,
	packageName: String,
	lastUpdateTimestamp: Long,
	needToCompleteIfSucceeded: Boolean,
	private val dbWriteSemaphore: BinarySemaphore
) : AbstractProgressSession<InstallFailure>(
	context, id, initialState, initialProgress,
	sessionDao, sessionFailureDao, sessionProgressDao,
	executor, handler,
	exceptionalFailureFactory = InstallFailure::Exceptional,
	notificationId, dbWriteSemaphore
) {

	private val apkFile = File(context.externalDir, "ackpine/sessions/$id/0.apk")

	init {
		completeIfSucceeded(initialState, packageName, lastUpdateTimestamp, needToCompleteIfSucceeded, executor)
	}

	private fun completeIfSucceeded(
		initialState: Session.State<InstallFailure>,
		packageName: String,
		lastUpdateTimestamp: Long,
		needToCompleteIfSucceeded: Boolean,
		executor: Executor
	) {
		if (initialState.isTerminal) {
			return
		}
		// Though it somewhat helps with self-update sessions, it's still faulty:
		// if app is force-stopped while the session is committed (not confirmed) and in the meantime
		// another installer updates the app, this session will be viewed as completed successfully.
		// We can check that initiating installer package is the same as ours, but then if this session
		// was successful, and before launching the app again it was updated by another installer,
		// the session will be stuck as committed. Sadly, without centralized system
		// sessions repository, such as android.content.pm.PackageInstaller, we can't reliably determine
		// whether the intent-based Ackpine session was really successful.
		val isSelfUpdate = initialState is Committed && context.packageName == packageName
		val isLastUpdateTimestampUpdated = getLastSelfUpdateTimestamp() > lastUpdateTimestamp
		val isSuccessfulSelfUpdate = isSelfUpdate && isLastUpdateTimestampUpdated
		if (isSuccessfulSelfUpdate && needToCompleteIfSucceeded) {
			complete(Succeeded)
		}
		if (isSuccessfulSelfUpdate) {
			executor.execute {
				dbWriteSemaphore.withPermit {
					lastUpdateTimestampDao.setLastUpdateTimestamp(id.toString(), getLastSelfUpdateTimestamp())
				}
			}
		}
	}

	private val Context.externalDir: File
		get() {
			val externalFilesDir = getExternalFilesDir(null)
			return if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED && externalFilesDir != null) {
				externalFilesDir
			} else {
				filesDir
			}
		}

	override fun prepare(cancellationSignal: CancellationSignal) {
		createApkCopy(cancellationSignal)
		val apkPackageName = context.packageManager
			.getPackageArchiveInfo(apkFile.absolutePath, 0)
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

	override fun launchConfirmation(notificationId: Int) {
		context.launchConfirmation<IntentBasedInstallActivity>(
			confirmation, notificationData,
			sessionId = id,
			notificationId,
			generateRequestCode(),
			CANCEL_CURRENT_FLAGS
		) { intent -> intent.putExtra(IntentBasedInstallActivity.APK_URI_KEY, getApkUri()) }
	}

	override fun doCleanup() {
		apkFile.delete()
	}

	override fun onCommitted() {
		progress = Progress((STREAM_COPY_PROGRESS_MAX * 0.9).roundToInt(), STREAM_COPY_PROGRESS_MAX)
	}

	override fun onCompleted(success: Boolean) {
		if (success) {
			progress = Progress(STREAM_COPY_PROGRESS_MAX, STREAM_COPY_PROGRESS_MAX)
		}
	}

	private fun getApkUri(): Uri {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
			return apkFile.toUri()
		}
		return FileProvider.getUriForFile(context, AckpineFileProvider.authority, apkFile)

	}

	private fun createApkCopy(cancellationSignal: CancellationSignal) {
		if (apkFile.exists()) {
			apkFile.delete()
		}
		apkFile.parentFile?.mkdirs()
		apkFile.createNewFile()
		val afd = context.openAssetFileDescriptor(apk, cancellationSignal)
			?: error("AssetFileDescriptor was null: $apk")
		afd.createInputStream().buffered().use { apkStream ->
			val outputStream = apkFile.outputStream()
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

	private fun getLastSelfUpdateTimestamp(): Long {
		return context.packageManager.getPackageInfo(context.packageName, 0).lastUpdateTime
	}

	private fun generateRequestCode() = Random.nextInt(2000000..3000000)
}