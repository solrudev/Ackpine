/*
 * Copyright (C) 2026 Ilya Fomichev
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
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ProviderInfo
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import androidx.core.content.FileProvider
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowContextWrapper
import org.robolectric.shadows.ShadowEnvironment
import ru.solrudev.ackpine.AckpineFileProvider
import ru.solrudev.ackpine.impl.helpers.SessionIdIntents
import ru.solrudev.ackpine.impl.helpers.concurrent.BinarySemaphore
import ru.solrudev.ackpine.impl.helpers.getParcelableExtraCompat
import ru.solrudev.ackpine.impl.installer.activity.IntentBasedInstallActivity
import ru.solrudev.ackpine.impl.testutil.DummyLastUpdateTimestampDao
import ru.solrudev.ackpine.impl.testutil.ImmediateExecutor
import ru.solrudev.ackpine.impl.testutil.RecordingSessionDao
import ru.solrudev.ackpine.impl.testutil.RecordingSessionProgressDao
import ru.solrudev.ackpine.impl.testutil.TestSessionFailureDao
import ru.solrudev.ackpine.impl.testutil.captureProgress
import ru.solrudev.ackpine.impl.testutil.captureStates
import ru.solrudev.ackpine.impl.testutil.createAckpineFile
import ru.solrudev.ackpine.impl.testutil.deleteAckpineFiles
import ru.solrudev.ackpine.impl.testutil.idleMainThread
import ru.solrudev.ackpine.installer.InstallFailure
import ru.solrudev.ackpine.runtime.R
import ru.solrudev.ackpine.session.Progress
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.session.parameters.Confirmation
import ru.solrudev.ackpine.session.parameters.NotificationData
import java.io.File
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private const val NOTIFICATION_ID = 7

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class IntentBasedInstallSessionTest {

	private val context: Context = ApplicationProvider.getApplicationContext()
	private val handler = Handler(Looper.getMainLooper())
	private val dbWriteSemaphore = BinarySemaphore()

	@BeforeTest
	fun setUp() {
		val authority = "${context.packageName}.AckpineFileProvider"
		val info = ProviderInfo().apply {
			this.authority = authority
			grantUriPermissions = true
			metaData = bundleOf(
				"android.support.FILE_PROVIDER_PATHS" to R.xml.ackpine_file_provider_paths
			)
		}
		Robolectric.buildContentProvider(AckpineFileProvider::class.java).create(info)
	}

	@AfterTest
	fun tearDown() {
		context.deleteAckpineFiles()
	}

	@Test
	fun launchCopiesApkAndTransitionsToAwaiting() {
		val sessionId = UUID.randomUUID()
		val apkFile = context.createAckpineFile("test/source-$sessionId.apk") { writeText("apk content") }
		val session = createSession(
			id = sessionId,
			apk = apkFile.toUri(),
			initialState = Session.State.Pending
		)
		val states = session.captureStates()

		session.launch()
		idleMainThread()

		val copiedApk = File(context.filesDir, "ackpine/sessions/$sessionId/0.apk")
		assertTrue(copiedApk.exists())
		assertEquals("apk content", copiedApk.readText())
		assertEquals(Session.State.Awaiting, states.last())
	}

	@Test
	fun launchUpdatesProgress() {
		val apkFile = context.createAckpineFile("test.apk")
		val session = createSession(apk = apkFile.toUri(), initialState = Session.State.Pending)
		val progressEvents = session.captureProgress()

		session.launch()
		idleMainThread()

		assertTrue(progressEvents.size > 1)
	}

	@Test
	fun launchReplacesExistingApkCopy() {
		val sessionId = UUID.randomUUID()
		val existingApk = File(context.filesDir, "ackpine/sessions/$sessionId/0.apk").apply {
			parentFile?.mkdirs()
			writeText("old")
		}
		val apkFile = context.createAckpineFile("test/source-replace-$sessionId.apk") { writeText("new") }
		val session = createSession(
			id = sessionId,
			apk = apkFile.toUri(),
			initialState = Session.State.Pending
		)

		session.launch()
		idleMainThread()

		assertEquals("new", existingApk.readText())
	}

	@Test
	fun commitImmediateLaunchesInstallActivity() {
		val sessionId = UUID.randomUUID()
		val session = createSession(
			id = sessionId,
			confirmation = Confirmation.IMMEDIATE,
			initialState = Session.State.Awaiting
		)

		session.commit()
		idleMainThread()

		val started = Shadow.extract<ShadowContextWrapper>(context).nextStartedActivity
		assertNotNull(started)
		assertEquals(IntentBasedInstallActivity::class.java.name, started.component?.className)
		val expectedApk = File(context.filesDir, "ackpine/sessions/$sessionId/0.apk")
		val expectedUri = FileProvider.getUriForFile(context, AckpineFileProvider.authority, expectedApk)
		val actualUri = started.getParcelableExtraCompat<Uri>(IntentBasedInstallActivity.APK_URI_KEY)
		assertEquals(expectedUri, actualUri)
		assertEquals(sessionId, SessionIdIntents.getSessionId(started, tag = "test"))
	}

	@Test
	fun commitDeferredPostsNotification() {
		val sessionId = UUID.randomUUID()
		val session = createSession(
			id = sessionId,
			confirmation = Confirmation.DEFERRED,
			initialState = Session.State.Awaiting
		)

		session.commit()
		idleMainThread()

		val notificationManager = shadowOf(context.getSystemService<NotificationManager>())
		assertNotNull(notificationManager.getNotification(sessionId.toString(), NOTIFICATION_ID))
	}

	@Test
	fun notifyCommittedUpdatesProgress() {
		val session = createSession(initialState = Session.State.Awaiting)
		val progressEvents = session.captureProgress()

		session.notifyCommitted()
		idleMainThread()

		assertEquals(90, progressEvents.last().progress)
	}

	@Test
	fun completeSucceededSetsProgressToMax() {
		val progressDao = RecordingSessionProgressDao()
		val session = createSession(
			progressDao = progressDao,
			initialState = Session.State.Awaiting
		)
		val progressEvents = session.captureProgress()

		session.complete(Session.State.Succeeded)
		idleMainThread()

		assertEquals(100, progressEvents.last().progress)
		assertEquals(Progress(100, 100), progressDao.progressUpdates.values.last().last())
	}

	@Test
	fun completeFailedDoesNotSetMaxProgress() {
		val session = createSession(initialState = Session.State.Awaiting)
		val progressEvents = session.captureProgress()

		session.complete(Session.State.Failed(InstallFailure.Generic("error")))
		idleMainThread()

		assertNotEquals(100, progressEvents.last().progress)
	}

	@Test
	fun cancelFromAwaitingCleansUpApkFile() {
		val sessionId = UUID.randomUUID()
		val apkFile = File(context.filesDir, "ackpine/sessions/$sessionId/0.apk").apply {
			parentFile?.mkdirs()
			writeText("apk content")
		}
		assertTrue(apkFile.exists())
		val session = createSession(
			id = sessionId,
			apk = Uri.EMPTY,
			initialState = Session.State.Awaiting
		)
		val states = session.captureStates()

		session.cancel()
		idleMainThread()

		assertEquals(Session.State.Cancelled, states.last())
		assertFalse(apkFile.exists())
	}

	@Test
	@Config(sdk = [Build.VERSION_CODES.M])
	fun commitImmediateLaunchesActivityWithFileUriPreN() {
		ShadowEnvironment.setExternalStorageState(Environment.MEDIA_MOUNTED)
		val sessionId = UUID.randomUUID()
		val session = createSession(
			id = sessionId,
			confirmation = Confirmation.IMMEDIATE,
			initialState = Session.State.Awaiting
		)

		session.commit()
		idleMainThread()

		val started = Shadow.extract<ShadowContextWrapper>(context).nextStartedActivity
		assertNotNull(started)
		assertEquals(IntentBasedInstallActivity::class.java.name, started.component?.className)

		val externalFilesDir = context.getExternalFilesDir(null)
		val expectedUri = File(externalFilesDir, "ackpine/sessions/$sessionId/0.apk").toUri()
		val actualUri = started.getParcelableExtraCompat<Uri>(IntentBasedInstallActivity.APK_URI_KEY)
		assertEquals(expectedUri, actualUri)
	}

	@Test
	@Config(sdk = [Build.VERSION_CODES.M])
	fun cancelCleansUpApkFromExternalStoragePreN() {
		ShadowEnvironment.setExternalStorageState(Environment.MEDIA_MOUNTED)
		val externalFilesDir = context.getExternalFilesDir(null)
		val sessionId = UUID.randomUUID()
		val apkFile = File(externalFilesDir, "ackpine/sessions/$sessionId/0.apk").apply {
			parentFile?.mkdirs()
			createNewFile()
		}
		assertTrue(apkFile.exists())
		val session = createSession(
			id = sessionId,
			apk = Uri.EMPTY,
			initialState = Session.State.Awaiting
		)
		val states = session.captureStates()

		session.cancel()
		idleMainThread()

		assertEquals(Session.State.Cancelled, states.last())
		assertFalse(apkFile.exists())
	}

	@Test
	@Config(sdk = [Build.VERSION_CODES.M])
	fun launchFailsWhenExternalStorageIsNotMountedPreN() {
		ShadowEnvironment.setExternalStorageState(Environment.MEDIA_UNMOUNTED)
		val session = createSession(
			apk = Uri.EMPTY,
			initialState = Session.State.Pending
		)

		session.launch()
		idleMainThread()

		val state = session.captureStates().last()
		assertIs<Session.State.Failed<InstallFailure>>(state)
		val failure = assertIs<InstallFailure.Exceptional>(state.failure)
		val message = assertNotNull(failure.exception.message)
		assertContains(message, "External storage is not available")
	}

	@Test
	@Config(sdk = [Build.VERSION_CODES.M])
	fun launchFailsWithStoragePermissionDeniedMessagePreN() {
		ShadowEnvironment.setExternalStorageState(Environment.MEDIA_UNMOUNTED)
		shadowOf(context as Application).denyPermissions(WRITE_EXTERNAL_STORAGE)
		val session = createSession(
			apk = Uri.EMPTY,
			initialState = Session.State.Pending
		)

		session.launch()
		idleMainThread()

		val state = session.captureStates().last()
		assertIs<Session.State.Failed<InstallFailure>>(state)
		val failure = assertIs<InstallFailure.Exceptional>(state.failure)
		val message = assertNotNull(failure.exception.message)
		assertContains(message, "External storage is not available")
		assertContains(message, "WRITE_EXTERNAL_STORAGE permission denied")
	}

	private fun createSession(
		apk: Uri = Uri.EMPTY,
		id: UUID = UUID.randomUUID(),
		confirmation: Confirmation = Confirmation.DEFERRED,
		initialState: Session.State<InstallFailure>,
		progressDao: RecordingSessionProgressDao = RecordingSessionProgressDao()
	) = IntentBasedInstallSession(
		context = context,
		apk = apk,
		id = id,
		initialState = initialState,
		initialProgress = Progress(),
		confirmation = confirmation,
		notificationData = NotificationData.DEFAULT,
		lastUpdateTimestampDao = DummyLastUpdateTimestampDao,
		sessionDao = RecordingSessionDao(),
		sessionFailureDao = TestSessionFailureDao(),
		sessionProgressDao = progressDao,
		executor = ImmediateExecutor,
		handler = handler,
		notificationId = NOTIFICATION_ID,
		dbWriteSemaphore = dbWriteSemaphore
	)
}