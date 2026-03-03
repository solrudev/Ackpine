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

package ru.solrudev.ackpine.impl.installer

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import kotlinx.coroutines.test.runTest
import org.junit.FixMethodOrder
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import ru.solrudev.ackpine.impl.ApkFixtures
import ru.solrudev.ackpine.impl.helpers.isPackageInstalled
import ru.solrudev.ackpine.impl.testutil.awaitWithTimeout
import ru.solrudev.ackpine.impl.testutil.test
import ru.solrudev.ackpine.installer.InstallFailure
import ru.solrudev.ackpine.installer.createSession
import ru.solrudev.ackpine.installer.parameters.InstallerType
import ru.solrudev.ackpine.installer.parameters.preapproval
import ru.solrudev.ackpine.remote.RemoteSession
import ru.solrudev.ackpine.remote.dsl.preapproval
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.session.parameters.Confirmation
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@LargeTest
@SdkSuppress(minSdkVersion = 34)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class PreapprovalFlowTest : AckpineInstallerTest() {

	@Test
	fun preapprovalCompletesSuccessfully() = runTest {
		preapprovalCompletesSuccessfully(fallbackToOnDemandApproval = false)
	}

	@Test
	fun preapprovalUnavailableWithFallbackCompletesSuccessfully() = runTest {
		ui.withDisabledPreapproval {
			preapprovalCompletesSuccessfully(fallbackToOnDemandApproval = true)
		}
	}

	@Test
	fun preapprovalUnavailableCompletesWithBlockedFailure() = runTest {
		ui.withDisabledPreapproval {
			val session = installer.createSession(ApkFixtures.fixtureUri()) {
				installerType = InstallerType.SESSION_BASED
				confirmation = Confirmation.IMMEDIATE
				preapproval(
					packageName = ApkFixtures.FIXTURE_PACKAGE_NAME,
					label = "Ackpine Fixture",
					locale = Locale.US
				)
			}
			val result = session.awaitWithTimeout()
			assertIs<Session.State.Failed<InstallFailure>>(result)
			assertIs<InstallFailure.Blocked>(result.failure)
			assertFalse(context.isPackageInstalled(ApkFixtures.FIXTURE_PACKAGE_NAME))
		}
	}

	@Test
	fun preapprovalRecoversAfterProcessDeath() = testProcessDeathRecovery(
		sessionFactory = {
			packageInstaller.createSession(ApkFixtures.fixtureUri()) {
				installerType = InstallerType.SESSION_BASED
				confirmation = Confirmation.IMMEDIATE
				preapproval(
					packageName = ApkFixtures.FIXTURE_PACKAGE_NAME,
					label = "Ackpine Fixture",
					locale = Locale.US
				)
			}
		},
		stateHandler = { session, state, job ->
			when (state) {
				RemoteSession.State.Pending -> session.launch()
				else -> job?.cancel()
			}
		},
		sessionConfirmation = {
			ui.clickInstallOrUpdate()
		},
		getSession = { sessionId ->
			packageInstaller.getSession(sessionId)
		}
	).also {
		assertTrue(context.isPackageInstalled(ApkFixtures.FIXTURE_PACKAGE_NAME))
	}

	private suspend fun preapprovalCompletesSuccessfully(fallbackToOnDemandApproval: Boolean) {
		val session = installer.createSession(ApkFixtures.fixtureUri()) {
			installerType = InstallerType.SESSION_BASED
			confirmation = Confirmation.IMMEDIATE
			preapproval(
				packageName = ApkFixtures.FIXTURE_PACKAGE_NAME,
				label = "Ackpine Fixture",
				locale = Locale.US
			) {
				this.fallbackToOnDemandApproval = fallbackToOnDemandApproval
			}
		}
		val result = session.test { ui.clickInstallOrUpdate() }
		assertEquals(Session.State.Succeeded, result)
		assertTrue(context.isPackageInstalled(ApkFixtures.FIXTURE_PACKAGE_NAME))
	}
}