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

import kotlinx.coroutines.test.runTest
import ru.solrudev.ackpine.impl.AckpineTest
import ru.solrudev.ackpine.impl.ApkFixtures
import ru.solrudev.ackpine.impl.testutil.isAndroid11
import ru.solrudev.ackpine.impl.uninstaller.activity.isPackageInstalled
import ru.solrudev.ackpine.remote.RemoteSession
import ru.solrudev.ackpine.uninstaller.parameters.UninstallerType
import kotlin.test.BeforeTest
import kotlin.test.assertFalse

open class AckpineUninstallerTest(
	allowUnknownSources: Boolean = !isAndroid11()
) : AckpineTest(allowUnknownSources) {

	@BeforeTest
	fun setUp() = runTest {
		installFixtureIfAbsent()
	}

	protected fun testProcessDeathConfirmationRecovery(
		uninstallerType: UninstallerType
	) = testProcessDeathRecovery(
		sessionFactory = {
			packageUninstaller.createImmediateSession(uninstallerType, ApkFixtures.FIXTURE_PACKAGE_NAME)
		},
		stateHandler = { session, state, job ->
			when (state) {
				RemoteSession.State.Pending -> session.launch()
				RemoteSession.State.Active -> session.launch()
				RemoteSession.State.Awaiting -> {
					session.commit()
					job?.cancel()
				}

				else -> { // no-op
				}
			}
		},
		sessionConfirmation = {
			ui.clickOk()
		},
		getSession = { sessionId ->
			packageUninstaller.getSession(sessionId)
		}
	).also {
		assertFalse(context.isPackageInstalled(ApkFixtures.FIXTURE_PACKAGE_NAME))
	}
}