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

import android.Manifest.permission.QUERY_ALL_PACKAGES
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import ru.solrudev.ackpine.impl.helpers.SessionIdIntents
import ru.solrudev.ackpine.impl.testutil.TestCompletableSession
import ru.solrudev.ackpine.impl.testutil.runScheduledMainThreadTasks
import ru.solrudev.ackpine.impl.uninstaller.PackageUninstallerImpl
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.uninstaller.UninstallFailure
import ru.solrudev.ackpine.uninstaller.parameters.VersionedUninstallPolicy
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class IntentBasedUninstallActivityTest {

	private val context: Context = ApplicationProvider.getApplicationContext()

	@Test
	fun replacedTargetFailsWithoutLaunchingConfirmation() {
		shadowOf(context.packageManager).installPackage(
			PackageInfo().apply {
				packageName = "com.example.app"
				setLongVersionCode(2L)
			}
		)
		assertUnavailableTargetFailsWithoutLaunchingConfirmation()
	}

	@Test
	fun missingTargetFailsWithoutLaunchingConfirmation() {
		shadowOf(context as Application).grantPermissions(QUERY_ALL_PACKAGES)
		assertUnavailableTargetFailsWithoutLaunchingConfirmation()
	}

	@Test
	fun resultCanceledCompletesWithAbortedFailure() {
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
	fun resultOkCompletesSucceeded() {
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
	fun resultFirstUserCompletesGenericFailure() {
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

	private fun assertUnavailableTargetFailsWithoutLaunchingConfirmation() {
		val sessionId = UUID.randomUUID()
		val session = TestCompletableSession<UninstallFailure>(sessionId)
		PackageUninstallerImpl.getInstance(context).addSession(sessionId, session)
		val intent = Intent(context, IntentBasedUninstallActivity::class.java)
			.putExtra(UninstallActivity.EXTRA_PACKAGE_NAME, "com.example.app")
			.putExtra(UninstallActivity.EXTRA_PACKAGE_VERSION_CODE, 1L)
			.putExtra(
				UninstallActivity.EXTRA_VERSIONED_UNINSTALL_POLICY,
				VersionedUninstallPolicy.ALLOW_NAME_ONLY.ordinal
			)
		SessionIdIntents.putSessionId(intent, sessionId)
		ActivityScenario.launch<IntentBasedUninstallActivity>(intent).use { scenario ->
			runScheduledMainThreadTasks()
			val result = assertIs<Session.State.Failed<UninstallFailure>>(session.completedState)
			assertIs<UninstallFailure.Generic>(result.failure)
			assertNull(shadowOf(context as Application).nextStartedActivityForResult)
			assertEquals(Lifecycle.State.DESTROYED, scenario.state)
		}
	}
}