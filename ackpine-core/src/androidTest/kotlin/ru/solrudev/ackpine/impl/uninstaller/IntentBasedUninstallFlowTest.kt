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

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.runner.RunWith
import ru.solrudev.ackpine.impl.ApkFixtures
import ru.solrudev.ackpine.impl.ExcludeAndroidTv
import ru.solrudev.ackpine.impl.OptInAndroid11
import ru.solrudev.ackpine.uninstaller.UninstallFailure
import ru.solrudev.ackpine.uninstaller.parameters.UninstallerType
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(AndroidJUnit4::class)
@LargeTest
class IntentBasedUninstallFlowTest : AckpineUninstallerTest() {

	@BeforeTest
	fun revokePermission() {
		ui.revokeInstallPermission(ApkFixtures.INSTALLER_PACKAGE_NAME)
	}

	@Test
	fun uninstallImmediateCompletesSuccessfully() = uninstallImmediateCompletesSuccessfully(
		UninstallerType.INTENT_BASED
	)

	@Test
	@ExcludeAndroidTv
	fun uninstallDeferredCompletesSuccessfully() = uninstallDeferredCompletesSuccessfully(
		UninstallerType.INTENT_BASED
	)

	@Test
	fun uninstallCancelCompletesWithGenericFailure() {
		uninstallCancelCompletesWithFailure<UninstallFailure.Generic>(
			UninstallerType.INTENT_BASED
		)
	}

	@Test
	fun uninstallNonexistentPackageCompletesWithGenericFailure() {
		uninstallNonexistentPackageCompletesWithFailure<UninstallFailure.Generic>(
			UninstallerType.INTENT_BASED
		)
	}

	@Test
	@OptInAndroid11
	fun uninstallConfirmationRecoversAfterProcessDeath() = testProcessDeathConfirmationRecovery(
		UninstallerType.INTENT_BASED
	)
}