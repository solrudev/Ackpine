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
import ru.solrudev.ackpine.impl.InstallPermissionRequest
import ru.solrudev.ackpine.impl.InstallPermissionRequest.Companion.STANDARD_SWITCH
import ru.solrudev.ackpine.impl.testutil.test
import ru.solrudev.ackpine.installer.InstallFailure
import ru.solrudev.ackpine.installer.createSession
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.session.parameters.Confirmation
import kotlin.test.Test
import kotlin.test.assertIs

@ExcludeAndroidTv
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 31)
@LargeTest
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class DismissConfirmationTest : AckpineInstallerTest(allowUnknownSources = false) {

	@Test
	fun dismissingDialogAbortsSession() = runTest {
		val session = installer.createSession(ApkFixtures.fixtureUri()) {
			confirmation = Confirmation.IMMEDIATE
		}
		val result = session.test {
			ui.grantInstallPermission(
				InstallPermissionRequest.installing(STANDARD_SWITCH, ApkFixtures.PACKAGE_NAME)
			)
			ui.dismissDialog()
			ui.pressBack()
		}
		assertIs<Session.State.Failed<InstallFailure>>(result)
		assertIs<InstallFailure.Aborted>(result.failure)
	}
}