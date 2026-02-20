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

package ru.solrudev.ackpine.impl.activity

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import ru.solrudev.ackpine.impl.helpers.SessionIdIntents
import ru.solrudev.ackpine.impl.installer.PackageInstallerImpl
import ru.solrudev.ackpine.impl.installer.activity.IntentBasedInstallActivity
import ru.solrudev.ackpine.impl.testutil.TestCompletableProgressSession
import ru.solrudev.ackpine.installer.InstallFailure
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests for [SessionCommitActivity] base class behavior.
 * Uses [IntentBasedInstallActivity] as the concrete implementation since SessionCommitActivity is abstract.
 */
@RunWith(RobolectricTestRunner::class)
class SessionCommitActivityTest {

	private val context: Context = ApplicationProvider.getApplicationContext()

	@Test
	fun configChangeDoesNotCallNotifyCommittedAgain() {
		val sessionId = UUID.randomUUID()
		val session = TestCompletableProgressSession<InstallFailure>(sessionId)
		PackageInstallerImpl.getInstance(context).addSession(sessionId, session)
		val intent = Intent(context, IntentBasedInstallActivity::class.java)
			.putExtra(IntentBasedInstallActivity.APK_URI_KEY, Uri.EMPTY)
		SessionIdIntents.putSessionId(intent, sessionId)
		ActivityScenario.launch<IntentBasedInstallActivity>(intent).use { scenario ->
			assertEquals(1, session.committedNotifiedCount)
			scenario.recreate()
			assertEquals(1, session.committedNotifiedCount)
		}
	}

	@Test
	fun configChangeDoesNotCompleteSession() {
		val sessionId = UUID.randomUUID()
		val session = TestCompletableProgressSession<InstallFailure>(sessionId)
		PackageInstallerImpl.getInstance(context).addSession(sessionId, session)
		val intent = Intent(context, IntentBasedInstallActivity::class.java)
			.putExtra(IntentBasedInstallActivity.APK_URI_KEY, Uri.EMPTY)
		SessionIdIntents.putSessionId(intent, sessionId)
		ActivityScenario.launch<IntentBasedInstallActivity>(intent).use { scenario ->
			scenario.recreate()
			assertNull(session.completedState)
		}
	}
}