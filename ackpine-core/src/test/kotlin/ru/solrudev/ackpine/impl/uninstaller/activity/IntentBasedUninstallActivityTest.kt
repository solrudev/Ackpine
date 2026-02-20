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

package ru.solrudev.ackpine.impl.uninstaller.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.os.Build
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import ru.solrudev.ackpine.impl.helpers.SessionIdIntents
import ru.solrudev.ackpine.impl.testutil.TestCompletableSession
import ru.solrudev.ackpine.impl.uninstaller.PackageUninstallerImpl
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.uninstaller.UninstallFailure
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@RunWith(RobolectricTestRunner::class)
class IntentBasedUninstallActivityTest {

	private val context: Context = ApplicationProvider.getApplicationContext()

	@Test
	@Config(sdk = [Build.VERSION_CODES.N])
	fun resultCanceledCompletesWithAbortedFailureOnPreQ() {
		val sessionId = UUID.randomUUID()
		val session = TestCompletableSession<UninstallFailure>(sessionId)
		PackageUninstallerImpl.getInstance(context).addSession(sessionId, session)
		val intent = Intent(context, IntentBasedUninstallActivity::class.java)
			.putExtra(UninstallActivity.EXTRA_PACKAGE_NAME, "com.example.app")
		SessionIdIntents.putSessionId(intent, sessionId)
		ActivityScenario.launch<IntentBasedUninstallActivity>(intent).use { scenario ->
			scenario.onActivity { it.onActivityResult(Activity.RESULT_CANCELED) }
			val result = session.completedState
			assertIs<Session.State.Failed<UninstallFailure>>(result)
			assertIs<UninstallFailure.Aborted>(result.failure)
		}
	}

	@Test
	@Config(sdk = [Build.VERSION_CODES.N])
	fun resultOkCompletesSucceededOnPreQ() {
		val sessionId = UUID.randomUUID()
		val session = TestCompletableSession<UninstallFailure>(sessionId)
		PackageUninstallerImpl.getInstance(context).addSession(sessionId, session)
		val intent = Intent(context, IntentBasedUninstallActivity::class.java)
			.putExtra(UninstallActivity.EXTRA_PACKAGE_NAME, "com.example.app")
		SessionIdIntents.putSessionId(intent, sessionId)
		ActivityScenario.launch<IntentBasedUninstallActivity>(intent).use { scenario ->
			scenario.onActivity { it.onActivityResult(Activity.RESULT_OK) }
			assertEquals(Session.State.Succeeded, session.completedState)
		}
	}

	@Test
	@Config(sdk = [Build.VERSION_CODES.N])
	fun resultFirstUserCompletesGenericFailureOnPreQ() {
		val sessionId = UUID.randomUUID()
		val session = TestCompletableSession<UninstallFailure>(sessionId)
		PackageUninstallerImpl.getInstance(context).addSession(sessionId, session)
		val intent = Intent(context, IntentBasedUninstallActivity::class.java)
			.putExtra(UninstallActivity.EXTRA_PACKAGE_NAME, "com.example.app")
		SessionIdIntents.putSessionId(intent, sessionId)
		ActivityScenario.launch<IntentBasedUninstallActivity>(intent).use { scenario ->
			scenario.onActivity { it.onActivityResult(Activity.RESULT_FIRST_USER) }
			val result = session.completedState
			assertIs<Session.State.Failed<UninstallFailure>>(result)
			assertIs<UninstallFailure.Generic>(result.failure)
		}
	}

	@Test
	fun packageRemovalCompletesSuccessfully() {
		// no package installed
		val sessionId = UUID.randomUUID()
		val session = TestCompletableSession<UninstallFailure>(sessionId)
		PackageUninstallerImpl.getInstance(context).addSession(sessionId, session)
		val intent = Intent(context, IntentBasedUninstallActivity::class.java)
			.putExtra(UninstallActivity.EXTRA_PACKAGE_NAME, "com.example.app")
		SessionIdIntents.putSessionId(intent, sessionId)
		ActivityScenario.launch<IntentBasedUninstallActivity>(intent).use { scenario ->
			scenario.onActivity { it.onActivityResult(Activity.RESULT_OK) }
			assertEquals(Session.State.Succeeded, session.completedState)
		}
	}

	@Test
	fun packageStillInstalledCompletesWithFailure() {
		val sessionId = UUID.randomUUID()
		val session = TestCompletableSession<UninstallFailure>(sessionId)
		val packageManager = shadowOf(context.packageManager)
		packageManager.installPackage(PackageInfo().apply { packageName = "com.example.app" })
		PackageUninstallerImpl.getInstance(context).addSession(sessionId, session)
		val intent = Intent(context, IntentBasedUninstallActivity::class.java)
			.putExtra(UninstallActivity.EXTRA_PACKAGE_NAME, "com.example.app")
		SessionIdIntents.putSessionId(intent, sessionId)
		ActivityScenario.launch<IntentBasedUninstallActivity>(intent).use { scenario ->
			scenario.onActivity { it.onActivityResult(Activity.RESULT_OK) }
			assertIs<Session.State.Failed<UninstallFailure>>(session.completedState)
		}
	}
}