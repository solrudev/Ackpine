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

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import ru.solrudev.ackpine.Ackpine
import ru.solrudev.ackpine.impl.helpers.SessionIdIntents
import ru.solrudev.ackpine.impl.installer.PackageInstallerImpl
import ru.solrudev.ackpine.impl.testutil.TestCompletableProgressSession
import ru.solrudev.ackpine.impl.testutil.TestPreapprovalSession
import ru.solrudev.ackpine.impl.testutil.drainMainThread
import ru.solrudev.ackpine.installer.InstallFailure
import ru.solrudev.ackpine.session.Session
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class SessionBasedInstallConfirmationActivityTest {

	private val context: Context = ApplicationProvider.getApplicationContext()

	@AfterTest
	fun tearDown() {
		Ackpine.setLogger(null)
	}

	@Test
	fun missingConfirmationActivityFailsSessionAndFinishesActivity() {
		val sessionId = UUID.randomUUID()
		val session = TestCompletableProgressSession<InstallFailure>(
			sessionId,
			exceptionalFailureFactory = InstallFailure::Exceptional
		)
		PackageInstallerImpl.getInstance(context).addSession(sessionId, session)
		val intent = Intent(context, SessionBasedInstallConfirmationActivity::class.java)
			.putExtra(Intent.EXTRA_INTENT, Intent("ru.solrudev.ackpine.test.MISSING_CONFIRMATION"))
			.putExtra(PackageInstaller.EXTRA_SESSION_ID, 1)
		SessionIdIntents.putSessionId(intent, sessionId)
		shadowOf(ApplicationProvider.getApplicationContext<Application>()).checkActivities(true)
		Robolectric.buildActivity(SessionBasedInstallConfirmationActivity::class.java, intent)
			.create()
			.use { controller ->
				drainMainThread()
				val state = assertIs<Session.State.Failed<InstallFailure>>(session.state)
				val failure = assertIs<InstallFailure.Exceptional>(state.failure)
				assertIs<ActivityNotFoundException>(failure.exception)
				assertTrue(controller.get().isFinishing)
			}
	}

	@Test
	fun regularInstallNotifiesCommitted() {
		val sessionId = UUID.randomUUID()
		val session = TestPreapprovalSession(sessionId)
		PackageInstallerImpl.getInstance(context).addSession(sessionId, session)
		val intent = Intent(context, SessionBasedInstallConfirmationActivity::class.java)
			.putExtra(Intent.EXTRA_INTENT, Intent())
			.putExtra(PackageInstaller.EXTRA_SESSION_ID, 1)
			.putExtra(PackageInstaller.EXTRA_PRE_APPROVAL, false)
		SessionIdIntents.putSessionId(intent, sessionId)
		ActivityScenario.launch<SessionBasedInstallConfirmationActivity>(intent).use {
			assertTrue(session.committedNotified)
			assertFalse(session.preapprovalChecked)
		}
	}

	@Test
	fun preapprovalDoesNotNotifyCommitted() {
		val sessionId = UUID.randomUUID()
		val session = TestPreapprovalSession(sessionId, isPreapprovalActive = true)
		PackageInstallerImpl.getInstance(context).addSession(sessionId, session)
		val intent = Intent(context, SessionBasedInstallConfirmationActivity::class.java)
			.putExtra(Intent.EXTRA_INTENT, Intent())
			.putExtra(PackageInstaller.EXTRA_SESSION_ID, 1)
			.putExtra(PackageInstaller.EXTRA_PRE_APPROVAL, true)
		SessionIdIntents.putSessionId(intent, sessionId)
		ActivityScenario.launch<SessionBasedInstallConfirmationActivity>(intent).use {
			assertFalse(session.committedNotified)
			assertTrue(session.preapprovalChecked)
		}
	}

	@Test
	fun stalePreapprovalFinishesActivity() {
		val sessionId = UUID.randomUUID()
		val session = TestPreapprovalSession(sessionId, isPreapprovalActive = false)
		PackageInstallerImpl.getInstance(context).addSession(sessionId, session)
		val intent = Intent(context, SessionBasedInstallConfirmationActivity::class.java)
			.putExtra(Intent.EXTRA_INTENT, Intent())
			.putExtra(PackageInstaller.EXTRA_SESSION_ID, 1)
			.putExtra(PackageInstaller.EXTRA_PRE_APPROVAL, true)
		SessionIdIntents.putSessionId(intent, sessionId)
		ActivityScenario.launch<SessionBasedInstallConfirmationActivity>(intent).use { scenario ->
			assertEquals(Lifecycle.State.DESTROYED, scenario?.state)
			assertFalse(session.committedNotified)
			assertTrue(session.preapprovalChecked)
		}
	}

	@Test
	@Config(sdk = [Build.VERSION_CODES.S])
	fun preapprovalFlagIsIgnoredOnOlderApi() {
		val sessionId = UUID.randomUUID()
		val session = TestPreapprovalSession(sessionId)
		PackageInstallerImpl.getInstance(context).addSession(sessionId, session)
		val intent = Intent(context, SessionBasedInstallConfirmationActivity::class.java)
			.putExtra(Intent.EXTRA_INTENT, Intent())
			.putExtra(PackageInstaller.EXTRA_SESSION_ID, 1)
			.putExtra(PackageInstaller.EXTRA_PRE_APPROVAL, true)
		SessionIdIntents.putSessionId(intent, sessionId)
		ActivityScenario.launch<SessionBasedInstallConfirmationActivity>(intent).use {
			assertTrue(session.committedNotified)
			assertFalse(session.preapprovalChecked)
		}
	}
}