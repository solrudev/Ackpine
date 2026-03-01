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
import org.junit.FixMethodOrder
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import ru.solrudev.ackpine.impl.ExcludeAndroidTv
import ru.solrudev.ackpine.impl.OptInAndroid11
import ru.solrudev.ackpine.installer.parameters.InstallerType
import ru.solrudev.ackpine.session.parameters.Confirmation
import kotlin.test.Test

@RunWith(AndroidJUnit4::class)
@LargeTest
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class IntentBasedInstallFlowTest : AckpineInstallerTest() {

	@Test
	fun installImmediateCompletesSuccessfully() = installImmediateCompletesSuccessfully(
		InstallerType.INTENT_BASED
	)

	@Test
	@ExcludeAndroidTv
	fun installDeferredCompletesSuccessfully() = installDeferredCompletesSuccessfully(
		InstallerType.INTENT_BASED
	)

	@Test
	fun installCancelCompletesWithAbortedFailure() = installCancelCompletesWithAbortedFailure(
		InstallerType.INTENT_BASED
	)

	@Test
	@OptInAndroid11
	fun installConfirmationRecoversAfterProcessDeath() = testProcessDeathConfirmationRecovery(
		InstallerType.INTENT_BASED
	)

	@Test
	@OptInAndroid11
	fun installPreparationsRecoverAfterProcessDeath() = testProcessDeathPreparationsRecovery(
		InstallerType.INTENT_BASED
	)

	@Test
	@OptInAndroid11
	fun selfUpdateCompletesSuccessfully() = testSelfUpdate(
		sessionFactory = { uri ->
			createSession(uri) {
				installerType = InstallerType.INTENT_BASED
				confirmation = Confirmation.IMMEDIATE
			}
		},
		sessionConfirmation = {
			ui.clickInstallOrUpdate()
		}
	)
}