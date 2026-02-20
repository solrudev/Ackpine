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

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.solrudev.ackpine.impl.helpers.SessionIdIntents
import ru.solrudev.ackpine.impl.installer.PackageInstallerImpl
import ru.solrudev.ackpine.impl.testutil.TestPreapprovalSession
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class SessionBasedInstallConfirmationActivityTest {

	private val context: Context = ApplicationProvider.getApplicationContext()

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
			assertFalse(session.preapprovalStarted)
		}
	}

	@Test
	fun preapprovalDoesNotNotifyCommitted() {
		val sessionId = UUID.randomUUID()
		val session = TestPreapprovalSession(sessionId)
		PackageInstallerImpl.getInstance(context).addSession(sessionId, session)
		val intent = Intent(context, SessionBasedInstallConfirmationActivity::class.java)
			.putExtra(Intent.EXTRA_INTENT, Intent())
			.putExtra(PackageInstaller.EXTRA_SESSION_ID, 1)
			.putExtra(PackageInstaller.EXTRA_PRE_APPROVAL, true)
		SessionIdIntents.putSessionId(intent, sessionId)
		ActivityScenario.launch<SessionBasedInstallConfirmationActivity>(intent).use {
			assertFalse(session.committedNotified)
			assertTrue(session.preapprovalStarted)
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
			assertFalse(session.preapprovalStarted)
		}
	}
}