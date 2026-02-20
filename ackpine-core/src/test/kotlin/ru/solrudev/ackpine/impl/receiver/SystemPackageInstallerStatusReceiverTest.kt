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

package ru.solrudev.ackpine.impl.receiver

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.os.Build
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowContextWrapper
import ru.solrudev.ackpine.AckpineThreadPool
import ru.solrudev.ackpine.impl.database.AckpineDatabase
import ru.solrudev.ackpine.impl.database.model.SessionEntity
import ru.solrudev.ackpine.impl.helpers.NotificationIntents
import ru.solrudev.ackpine.impl.helpers.SessionIdIntents
import ru.solrudev.ackpine.impl.installer.activity.IntentBasedInstallActivity
import ru.solrudev.ackpine.impl.installer.activity.SessionBasedInstallConfirmationActivity
import ru.solrudev.ackpine.impl.installer.session.PackageInstallerStatus
import ru.solrudev.ackpine.impl.testutil.TestCompletableProgressSession
import ru.solrudev.ackpine.impl.testutil.TestPreapprovalSession
import ru.solrudev.ackpine.impl.testutil.createInstallSessionEntity
import ru.solrudev.ackpine.impl.testutil.idleMainThread
import ru.solrudev.ackpine.installer.InstallFailure
import ru.solrudev.ackpine.installer.parameters.InstallerType
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.session.parameters.Confirmation
import ru.solrudev.ackpine.session.parameters.NotificationData
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class SystemPackageInstallerStatusReceiverTest {

	private val context: Context = ApplicationProvider.getApplicationContext()

	@Test
	fun pendingUserActionWithoutConfirmationIntentCompletesExceptionally() {
		val sessionId = UUID.randomUUID()
		val session = TestCompletableProgressSession<InstallFailure>(
			id = sessionId,
			exceptionalFailureFactory = InstallFailure::Exceptional
		)
		val receiver = TestInstallStatusReceiver(session)
		registerReceiver(receiver)
		val intent = createStatusIntent(
			action = receiver.action,
			sessionId = sessionId,
			status = PackageInstaller.STATUS_PENDING_USER_ACTION,
			confirmation = Confirmation.IMMEDIATE
		)
		context.sendBroadcast(intent)
		idleMainThread()

		assertNotNull(session.completionException)
		assertIs<Session.State.Failed<InstallFailure>>(session.state)
		context.unregisterReceiver(receiver)
	}

	@Test
	fun immediateConfirmationLaunchesWrapperActivity() {
		val sessionId = UUID.randomUUID()
		val session = TestCompletableProgressSession<InstallFailure>(sessionId)
		val receiver = TestInstallStatusReceiver(session)
		registerReceiver(receiver)
		val intent = createStatusIntent(
			action = receiver.action,
			sessionId = sessionId,
			status = PackageInstaller.STATUS_PENDING_USER_ACTION,
			confirmationIntent = Intent(),
			confirmation = Confirmation.IMMEDIATE
		)
		context.sendBroadcast(intent)
		idleMainThread()

		val started = Shadow.extract<ShadowContextWrapper>(context).nextStartedActivity
		assertNotNull(started)
		assertEquals(SessionBasedInstallConfirmationActivity::class.java.name, started.component?.className)
		context.unregisterReceiver(receiver)
	}

	@Test
	fun deferredConfirmationPostsNotification() {
		val sessionId = UUID.randomUUID()
		val session = TestCompletableProgressSession<InstallFailure>(sessionId)
		val receiver = TestInstallStatusReceiver(session)
		registerReceiver(receiver)
		val intent = createStatusIntent(
			action = receiver.action,
			sessionId = sessionId,
			status = PackageInstaller.STATUS_PENDING_USER_ACTION,
			confirmationIntent = Intent(),
			confirmation = Confirmation.DEFERRED,
			notificationData = NotificationData.DEFAULT,
			notificationId = 42
		)
		context.sendBroadcast(intent)
		idleMainThread()

		val notificationManager = shadowOf(context.getSystemService<NotificationManager>())
		val notification = notificationManager.getNotification(sessionId.toString(), 42)
		assertNotNull(notification)
		context.unregisterReceiver(receiver)
	}

	@Test
	fun statusSuccessCompletesSessionSucceeded() {
		val sessionId = UUID.randomUUID()
		val session = TestCompletableProgressSession<InstallFailure>(sessionId)
		val receiver = TestInstallStatusReceiver(session)
		registerReceiver(receiver)
		val intent = createStatusIntent(
			action = receiver.action,
			sessionId = sessionId,
			status = PackageInstaller.STATUS_SUCCESS
		)
		context.sendBroadcast(intent)
		idleMainThread()

		assertEquals(Session.State.Succeeded, session.completedState)
		assertNull(session.completionException)
		context.unregisterReceiver(receiver)
	}

	@Test
	fun statusFailureCompletesSessionFailed() {
		val sessionId = UUID.randomUUID()
		val session = TestCompletableProgressSession<InstallFailure>(sessionId)
		val receiver = TestInstallStatusReceiver(session)
		registerReceiver(receiver)
		val intent = createStatusIntent(
			action = receiver.action,
			sessionId = sessionId,
			status = PackageInstaller.STATUS_FAILURE
		).putExtra(PackageInstaller.EXTRA_STATUS_MESSAGE, "failure")
		context.sendBroadcast(intent)
		idleMainThread()

		val completedState = session.completedState
		assertNotNull(completedState)
		assertIs<Session.State.Failed<InstallFailure>>(completedState)
		assertEquals(InstallFailure.Generic("failure"), completedState.failure)
		context.unregisterReceiver(receiver)
	}

	@Test
	fun wrongIntentActionIsIgnored() {
		val sessionId = UUID.randomUUID()
		val session = TestCompletableProgressSession<InstallFailure>(sessionId)
		val filteredAction = "ru.solrudev.ackpine.TEST_FILTERED_ACTION"
		val receiver = IgnoringActionReceiver(
			session = session,
			expectedAction = "ru.solrudev.ackpine.TEST_EXPECTED_ACTION"
		)
		ContextCompat.registerReceiver(
			context,
			receiver,
			IntentFilter(filteredAction),
			ContextCompat.RECEIVER_NOT_EXPORTED
		)
		val intent = Intent(filteredAction)
			.putExtra(PackageInstaller.EXTRA_SESSION_ID, 1)
			.putExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_SUCCESS)
		SessionIdIntents.putSessionId(intent, sessionId)
		context.sendBroadcast(intent)
		idleMainThread()

		assertEquals(0, receiver.getAckpineSessionCalls)
		context.unregisterReceiver(receiver)
	}

	@Test
	fun preapprovalSuccessNotifiesListener() {
		val sessionId = UUID.randomUUID()
		val session = TestPreapprovalSession(sessionId)
		val receiver = TestInstallStatusReceiver(session)
		registerReceiver(receiver)
		val intent = createStatusIntent(
			action = receiver.action,
			sessionId = sessionId,
			status = PackageInstaller.STATUS_SUCCESS,
			preapproval = true
		)
		context.sendBroadcast(intent)
		idleMainThread()

		assertTrue(session.preapprovalSucceeded)
		assertNull(session.preapprovalFailure)
		context.unregisterReceiver(receiver)
	}

	@Test
	fun preapprovalFailureNotifiesListener() {
		val sessionId = UUID.randomUUID()
		val session = TestPreapprovalSession(sessionId)
		val receiver = TestInstallStatusReceiver(session)
		registerReceiver(receiver)
		val intent = createStatusIntent(
			action = receiver.action,
			sessionId = sessionId,
			status = PackageInstaller.STATUS_FAILURE
		).putExtra(
			"android.content.pm.extra.LEGACY_STATUS",
			PackageInstallerStatus.INSTALL_FAILED_PRE_APPROVAL_NOT_AVAILABLE.legacyStatus
		)
		context.sendBroadcast(intent)
		idleMainThread()

		assertNotNull(session.preapprovalFailure)
		context.unregisterReceiver(receiver)
	}

	@Test
	fun pendingUserActionPassesPreapprovalFlagToWrapperActivity() {
		val sessionId = UUID.randomUUID()
		val session = TestCompletableProgressSession<InstallFailure>(sessionId)
		val receiver = TestInstallStatusReceiver(session)
		registerReceiver(receiver)
		val intent = createStatusIntent(
			action = receiver.action,
			sessionId = sessionId,
			status = PackageInstaller.STATUS_PENDING_USER_ACTION,
			confirmationIntent = Intent(),
			confirmation = Confirmation.IMMEDIATE,
			preapproval = true
		)
		context.sendBroadcast(intent)
		idleMainThread()

		val started = Shadow.extract<ShadowContextWrapper>(context).nextStartedActivity
		assertNotNull(started)
		assertTrue(started.getBooleanExtra(PackageInstaller.EXTRA_PRE_APPROVAL, false))
		context.unregisterReceiver(receiver)
	}

	@Test
	@Config(sdk = [Build.VERSION_CODES.S_V2])
	fun pendingUserActionNoRequireUserActionPersistsConfirmationLaunched() = runTest {
		val sessionId = UUID.randomUUID()
		val session = TestCompletableProgressSession<InstallFailure>(sessionId)
		val receiver = TestInstallStatusReceiver(session)
		registerReceiver(receiver)

		val database = AckpineDatabase.getInstance(context, AckpineThreadPool)
		withContext(Dispatchers.IO) {
			database.installSessionDao().insertInstallSession(
				createInstallSessionEntity(
					id = sessionId.toString(),
					state = SessionEntity.State.PENDING,
					installerType = InstallerType.DEFAULT,
					uris = emptyList()
				)
			)
		}

		val intent = createStatusIntent(
			action = receiver.action,
			sessionId = sessionId,
			status = PackageInstaller.STATUS_PENDING_USER_ACTION,
			confirmationIntent = Intent(),
			confirmation = Confirmation.IMMEDIATE,
			requireUserAction = false
		)
		context.sendBroadcast(intent)
		idleMainThread()

		val wasConfirmationLaunched = withContext(Dispatchers.IO) {
			database
				.installSessionDao()
				.getInstallSession(sessionId.toString())
				?.wasConfirmationLaunched
		}

		assertNotNull(wasConfirmationLaunched)
		assertTrue(wasConfirmationLaunched)

		context.unregisterReceiver(receiver)
	}

	private fun registerReceiver(receiver: TestInstallStatusReceiver) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			ContextCompat.registerReceiver(
				context,
				receiver,
				IntentFilter(receiver.action),
				ContextCompat.RECEIVER_NOT_EXPORTED
			)
			return
		}
		@Suppress("DEPRECATION")
		@SuppressLint("UnspecifiedRegisterReceiverFlag")
		context.registerReceiver(receiver, IntentFilter(receiver.action))
	}

	private class TestInstallStatusReceiver(
		private val session: TestCompletableProgressSession<InstallFailure>?
	) : SystemPackageInstallerStatusReceiver<InstallFailure>(
		confirmationWrapperActivityClass = SessionBasedInstallConfirmationActivity::class.java,
		tag = "TestReceiver"
	) {

		val action = "ru.solrudev.ackpine.TEST_ACTION"

		override fun getAckpineSessionAsync(
			context: Context,
			ackpineSessionId: UUID
		) = immediateFuture(session)

		override fun getFailure(
			status: Int,
			message: String?,
			otherPackageName: String?,
			storagePath: String?
		) = InstallFailure.Generic(message)

		override fun getAction(context: Context) = action

		private fun <T> immediateFuture(value: T) = CallbackToFutureAdapter.getFuture { completer ->
			completer.set(value)
		}
	}

	private class IgnoringActionReceiver(
		private val session: TestCompletableProgressSession<InstallFailure>?,
		private val expectedAction: String
	) : SystemPackageInstallerStatusReceiver<InstallFailure>(
		confirmationWrapperActivityClass = IntentBasedInstallActivity::class.java,
		tag = "TestReceiver"
	) {

		var getAckpineSessionCalls: Int = 0
			private set

		override fun getAckpineSessionAsync(
			context: Context,
			ackpineSessionId: UUID
		) = immediateFuture(session).also {
			getAckpineSessionCalls++
		}

		override fun getFailure(
			status: Int,
			message: String?,
			otherPackageName: String?,
			storagePath: String?
		) = InstallFailure.Generic(message)

		override fun getAction(context: Context) = expectedAction

		private fun <T> immediateFuture(value: T) = CallbackToFutureAdapter.getFuture { completer ->
			completer.set(value)
		}
	}

	private fun createStatusIntent(
		action: String,
		sessionId: UUID,
		nativeSessionId: Int = 1,
		status: Int,
		confirmationIntent: Intent? = null,
		confirmation: Confirmation? = null,
		notificationData: NotificationData? = null,
		notificationId: Int? = null,
		preapproval: Boolean = false,
		requireUserAction: Boolean = true
	): Intent {
		val intent = Intent(action)
			.putExtra(PackageInstaller.EXTRA_SESSION_ID, nativeSessionId)
			.putExtra(PackageInstaller.EXTRA_STATUS, status)
		if (confirmationIntent != null) {
			intent.putExtra(Intent.EXTRA_INTENT, confirmationIntent)
		}
		if (confirmation != null) {
			intent.putExtra(SystemPackageInstallerStatusReceiver.EXTRA_CONFIRMATION, confirmation.ordinal)
		}
		if (notificationData != null && notificationId != null) {
			NotificationIntents.putNotification(intent, notificationData, notificationId)
		}
		if (preapproval) {
			intent.putExtra(PackageInstaller.EXTRA_PRE_APPROVAL, true)
		}
		if (!requireUserAction) {
			intent.putExtra(SystemPackageInstallerStatusReceiver.EXTRA_REQUIRE_USER_ACTION, false)
		}
		SessionIdIntents.putSessionId(intent, sessionId)
		return intent
	}
}