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

package ru.solrudev.ackpine.impl.uninstaller

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import ru.solrudev.ackpine.impl.ApkFixtures
import ru.solrudev.ackpine.impl.OptInAndroid11
import ru.solrudev.ackpine.impl.testutil.test
import ru.solrudev.ackpine.impl.uninstaller.activity.isPackageInstalled
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.session.parameters.Confirmation
import ru.solrudev.ackpine.uninstaller.UninstallFailure
import ru.solrudev.ackpine.uninstaller.createSession
import ru.solrudev.ackpine.uninstaller.parameters.UninstallerType
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

@RunWith(AndroidJUnit4::class)
@LargeTest
class IntentBasedUninstallFlowTest : AckpineUninstallerTest() {

	@BeforeTest
	fun revokePermission() {
		ui.revokeInstallPermission(ApkFixtures.INSTALLER_PACKAGE_NAME)
	}

	@Test
	fun uninstallCompletesSuccessfully() = runTest {
		val uninstallSession = uninstaller.createSession(ApkFixtures.FIXTURE_PACKAGE_NAME) {
			uninstallerType = UninstallerType.INTENT_BASED
			confirmation = Confirmation.IMMEDIATE
		}
		val result = uninstallSession.test { ui.clickOk() }
		assertEquals(Session.State.Succeeded, result)
		assertFalse(context.isPackageInstalled(ApkFixtures.FIXTURE_PACKAGE_NAME))
	}

	@Test
	fun uninstallCancelCompletesWithFailure() = runTest {
		val uninstallSession = uninstaller.createSession(ApkFixtures.FIXTURE_PACKAGE_NAME) {
			uninstallerType = UninstallerType.INTENT_BASED
			confirmation = Confirmation.IMMEDIATE
		}
		val result = uninstallSession.test { ui.clickCancel() }
		assertIs<Session.State.Failed<UninstallFailure>>(result)
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
			assertIs<UninstallFailure.Aborted>(result.failure)
		} else {
			assertIs<UninstallFailure.Generic>(result.failure)
		}
	}

	@Test
	@OptInAndroid11
	fun uninstallConfirmationRecoversAfterProcessDeath() = testProcessDeathConfirmationRecovery(
		UninstallerType.INTENT_BASED
	)
}