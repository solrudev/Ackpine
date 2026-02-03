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
import ru.solrudev.ackpine.impl.ExcludeAndroidTv
import ru.solrudev.ackpine.impl.OptInAndroid11
import ru.solrudev.ackpine.impl.testutil.test
import ru.solrudev.ackpine.impl.uninstaller.activity.isPackageInstalled
import ru.solrudev.ackpine.installer.createSession
import ru.solrudev.ackpine.installer.parameters.InstallerType
import ru.solrudev.ackpine.resources.ResolvableString
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.session.parameters.Confirmation
import ru.solrudev.ackpine.session.parameters.notification
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@LargeTest
@SdkSuppress(minSdkVersion = 21)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class SessionBasedInstallFlowTest : AckpineInstallerTest() {

	@Test
	fun installImmediateCompletesSuccessfully() = runTest {
		val session = installer.createSession(ApkFixtures.fixtureUri()) {
			installerType = InstallerType.SESSION_BASED
			confirmation = Confirmation.IMMEDIATE
		}
		val result = session.test { ui.clickInstallOrUpdate() }
		assertEquals(Session.State.Succeeded, result)
		assertTrue(context.isPackageInstalled(ApkFixtures.FIXTURE_PACKAGE_NAME))
	}

	@Test
	@ExcludeAndroidTv
	fun installDeferredCompletesSuccessfully() = runTest {
		val session = installer.createSession(ApkFixtures.fixtureUri()) {
			installerType = InstallerType.SESSION_BASED
			confirmation = Confirmation.DEFERRED
			notification {
				title = ResolvableString.raw("Ackpine install")
			}
		}

		val result = session.test {
			ui.clickNotification("Ackpine install")
			ui.clickInstallOrUpdate()
		}

		assertEquals(Session.State.Succeeded, result)
	}

	@Test
	@OptInAndroid11
	fun installConfirmationRecoversAfterProcessDeath() = testProcessDeathConfirmationRecovery(
		InstallerType.SESSION_BASED
	)

	@Test
	@OptInAndroid11
	fun installPreparationsRecoverAfterProcessDeath() = testProcessDeathPreparationsRecovery(
		InstallerType.SESSION_BASED
	)

	@Test
	@OptInAndroid11
	fun selfUpdateCompletesSuccessfully() = testSelfUpdate(
		sessionFactory = { uri ->
			createImmediateSession(InstallerType.SESSION_BASED, uri)
		},
		sessionConfirmation = {
			ui.clickInstallOrUpdate()
		}
	)
}