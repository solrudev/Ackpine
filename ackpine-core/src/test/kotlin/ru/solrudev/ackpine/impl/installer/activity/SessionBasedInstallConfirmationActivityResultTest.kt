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

package ru.solrudev.ackpine.impl.installer.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import ru.solrudev.ackpine.impl.helpers.SessionIdIntents
import ru.solrudev.ackpine.impl.installer.CommitProgressValueHolder
import ru.solrudev.ackpine.impl.installer.PackageInstallerImpl
import ru.solrudev.ackpine.impl.testutil.TestPreapprovalSession
import ru.solrudev.ackpine.impl.testutil.idleMainThread
import ru.solrudev.ackpine.impl.testutil.runScheduledMainThreadTasks
import ru.solrudev.ackpine.installer.InstallFailure
import ru.solrudev.ackpine.session.Session
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class SessionBasedInstallConfirmationActivityResultTest {

	private val context: Context = ApplicationProvider.getApplicationContext()

	@BeforeTest
	fun setUp() {
		CommitProgressValueHolder.clear(context)
	}

	@Test
	fun activityCancelledAbortsSession() {
		ensureCommitProgressValueCached()
		val sessionId = UUID.randomUUID()
		val session = TestPreapprovalSession(sessionId)
		PackageInstallerImpl.getInstance(context).addSession(sessionId, session)
		val intent = Intent(context, SessionBasedInstallConfirmationActivity::class.java)
			.putExtra(Intent.EXTRA_INTENT, Intent())
			.putExtra(PackageInstaller.EXTRA_SESSION_ID, 1)
		SessionIdIntents.putSessionId(intent, sessionId)
		ActivityScenario.launch<SessionBasedInstallConfirmationActivity>(intent).use { scenario ->
			scenario.onActivity { it.onActivityResult(Activity.RESULT_CANCELED) }
			idleMainThread()
			val completedState = session.completedState
			assertNotNull(completedState)
			assertIs<Session.State.Failed<InstallFailure>>(completedState)
			assertIs<InstallFailure.Aborted>(completedState.failure)
		}
	}

	@Test
	fun preapprovalResultDoesNotAbortSession() {
		ensureCommitProgressValueCached()
		val sessionId = UUID.randomUUID()
		val session = TestPreapprovalSession(sessionId)
		PackageInstallerImpl.getInstance(context).addSession(sessionId, session)
		val intent = Intent(context, SessionBasedInstallConfirmationActivity::class.java)
			.putExtra(Intent.EXTRA_INTENT, Intent())
			.putExtra(PackageInstaller.EXTRA_SESSION_ID, 1)
			.putExtra(PackageInstaller.EXTRA_PRE_APPROVAL, true)
		SessionIdIntents.putSessionId(intent, sessionId)
		ActivityScenario.launch<SessionBasedInstallConfirmationActivity>(intent).use { scenario ->
			scenario.onActivity { it.onActivityResult(Activity.RESULT_CANCELED) }
			idleMainThread()
			scenario.onActivity {
				assertTrue(it.isFinishing)
				assertNull(session.completedState)
			}
		}
	}

	@Test
	fun preapprovalResultOkFinishesActivity() {
		ensureCommitProgressValueCached()
		val sessionId = UUID.randomUUID()
		val session = TestPreapprovalSession(sessionId)
		PackageInstallerImpl.getInstance(context).addSession(sessionId, session)
		val intent = Intent(context, SessionBasedInstallConfirmationActivity::class.java)
			.putExtra(Intent.EXTRA_INTENT, Intent())
			.putExtra(PackageInstaller.EXTRA_SESSION_ID, 1)
			.putExtra(PackageInstaller.EXTRA_PRE_APPROVAL, true)
		SessionIdIntents.putSessionId(intent, sessionId)
		ActivityScenario.launch<SessionBasedInstallConfirmationActivity>(intent).use { scenario ->
			scenario.onActivity { it.onActivityResult(Activity.RESULT_OK) }
			idleMainThread()
			scenario.onActivity {
				assertTrue(it.isFinishing)
				assertNull(session.completedState)
			}
		}
	}

	@Test
	@Config(sdk = [Build.VERSION_CODES.N]) // Use API < 26 so canInstallPackages() returns true
	fun deadSessionCompletesWithFailureAfterTimeout() {
		ensureSessionIsNotStuck()
		val sessionId = UUID.randomUUID()
		val session = TestPreapprovalSession(sessionId)
		PackageInstallerImpl.getInstance(context).addSession(sessionId, session)
		// Use a session ID that doesn't exist in the PackageInstaller (simulating dead session)
		val intent = Intent(context, SessionBasedInstallConfirmationActivity::class.java)
			.putExtra(Intent.EXTRA_INTENT, Intent())
			.putExtra(PackageInstaller.EXTRA_SESSION_ID, 999) // Non-existent session
		SessionIdIntents.putSessionId(intent, sessionId)
		ActivityScenario.launch<SessionBasedInstallConfirmationActivity>(intent).use { scenario ->
			scenario.onActivity { it.onActivityResult(Activity.RESULT_OK) }
			// Advance time to trigger the deadSessionCompletionRunnable (1000ms delay)
			runScheduledMainThreadTasks()
			val completedState = session.completedState
			assertNotNull(completedState)
			assertIs<Session.State.Failed<InstallFailure>>(completedState)
			assertIs<InstallFailure.Generic>(completedState.failure)
			assertEquals("Session 999 is dead.", completedState.failure.message)
		}
	}

	@Test
	@Config(sdk = [Build.VERSION_CODES.N])
	fun onDestroyWhenFinishingRemovesDeadSessionCallback() {
		ensureCommitProgressValueCached()
		val sessionId = UUID.randomUUID()
		val session = TestPreapprovalSession(sessionId)
		PackageInstallerImpl.getInstance(context).addSession(sessionId, session)
		val intent = Intent(context, SessionBasedInstallConfirmationActivity::class.java)
			.putExtra(Intent.EXTRA_INTENT, Intent())
			.putExtra(PackageInstaller.EXTRA_SESSION_ID, 999)
		SessionIdIntents.putSessionId(intent, sessionId)
		ActivityScenario.launch<SessionBasedInstallConfirmationActivity>(intent).use { scenario ->
			// Trigger the else branch which posts deadSessionCompletionRunnable
			scenario.onActivity { it.onActivityResult(Activity.RESULT_OK) }
			idleMainThread()
			scenario.onActivity { it.finish() }
		}
		runScheduledMainThreadTasks()
		// Session should not be completed because callback should have been removed on destroy when finishing
		assertNull(session.completedState)
	}

	@Test
	fun activityResultOkWithAliveSessionFinishes() {
		ensureCommitProgressValueCached()
		val sessionId = UUID.randomUUID()
		val session = TestPreapprovalSession(sessionId)
		PackageInstallerImpl.getInstance(context).addSession(sessionId, session)
		val packageInstaller = context.packageManager.packageInstaller
		val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
		val nativeSessionId = packageInstaller.createSession(params)
		shadowOf(packageInstaller).setSessionProgress(nativeSessionId, 0.5f)
		val intent = Intent(context, SessionBasedInstallConfirmationActivity::class.java)
			.putExtra(Intent.EXTRA_INTENT, Intent())
			.putExtra(PackageInstaller.EXTRA_SESSION_ID, 1)
		SessionIdIntents.putSessionId(intent, sessionId)
		ActivityScenario.launch<SessionBasedInstallConfirmationActivity>(intent).use { scenario ->
			scenario.onActivity { it.onActivityResult(Activity.RESULT_OK) }
			idleMainThread()
			// Session is alive and not stuck, should just finish without completing session
			scenario.onActivity {
				assertTrue(it.isFinishing)
				assertNull(session.completedState)
			}
			packageInstaller.abandonSession(nativeSessionId)
		}
	}

	@Test
	fun activityCancelledWithAliveSessionAbortsWithUserCancelled() {
		ensureSessionIsNotStuck()
		val sessionId = UUID.randomUUID()
		val session = TestPreapprovalSession(sessionId)
		PackageInstallerImpl.getInstance(context).addSession(sessionId, session)
		val packageInstaller = context.packageManager.packageInstaller
		val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
		val nativeSessionId = packageInstaller.createSession(params)
		shadowOf(packageInstaller).setSessionProgress(nativeSessionId, 0.5f)
		val intent = Intent(context, SessionBasedInstallConfirmationActivity::class.java)
			.putExtra(Intent.EXTRA_INTENT, Intent())
			.putExtra(PackageInstaller.EXTRA_SESSION_ID, nativeSessionId)
		SessionIdIntents.putSessionId(intent, sessionId)
		ActivityScenario.launch<SessionBasedInstallConfirmationActivity>(intent).use { scenario ->
			scenario.onActivity { it.onActivityResult(Activity.RESULT_CANCELED) }
			idleMainThread()
			val completedState = session.completedState
			assertNotNull(completedState)
			assertIs<Session.State.Failed<InstallFailure>>(completedState)
			assertIs<InstallFailure.Aborted>(completedState.failure)
			packageInstaller.abandonSession(nativeSessionId)
		}
	}

	// Allows to skip thread pool dispatch
	private fun ensureCommitProgressValueCached() {
		CommitProgressValueHolder.putIfAbsent(context) { 0f }
	}

	private fun ensureSessionIsNotStuck() {
		CommitProgressValueHolder.putIfAbsent(context) { 1f }
	}
}