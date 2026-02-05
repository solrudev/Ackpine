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

import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import ru.solrudev.ackpine.impl.AckpineTest
import ru.solrudev.ackpine.impl.ApkFixtures
import ru.solrudev.ackpine.impl.InstallPermissionRequest
import ru.solrudev.ackpine.impl.InstallPermissionRequest.Companion.STANDARD_SWITCH
import ru.solrudev.ackpine.impl.testutil.awaitWithTimeout
import ru.solrudev.ackpine.impl.testutil.isAndroid11
import ru.solrudev.ackpine.impl.testutil.isTv
import ru.solrudev.ackpine.impl.testutil.test
import ru.solrudev.ackpine.impl.uninstaller.activity.isPackageInstalled
import ru.solrudev.ackpine.installer.parameters.InstallerType
import ru.solrudev.ackpine.remote.AckpineRemoteService
import ru.solrudev.ackpine.remote.RemotePackageInstaller
import ru.solrudev.ackpine.remote.RemoteSession
import kotlin.test.AfterTest
import kotlin.test.assertEquals
import kotlin.test.assertTrue

open class AckpineInstallerTest(
	allowUnknownSources: Boolean = !isAndroid11()
) : AckpineTest(allowUnknownSources) {

	@AfterTest
	fun tearDown() = runTest {
		uninstallFixtureIfPresent()
	}

	protected fun testProcessDeathConfirmationRecovery(
		installerType: InstallerType
	) = testProcessDeathRecovery(
		sessionFactory = {
			packageInstaller.createImmediateSession(installerType, ApkFixtures.fixtureUri())
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
			ui.clickInstallOrUpdate()
		},
		getSession = { sessionId ->
			packageInstaller.getSession(sessionId)
		}
	).also {
		assertTrue(context.isPackageInstalled(ApkFixtures.FIXTURE_PACKAGE_NAME))
	}

	protected fun testProcessDeathPreparationsRecovery(
		installerType: InstallerType
	) = testProcessDeathRecovery(
		sessionFactory = {
			packageInstaller.createImmediateSession(installerType, ApkFixtures.fixtureUri())
		},
		stateHandler = { session, state, job ->
			when (state) {
				RemoteSession.State.Pending -> session.launch()
				RemoteSession.State.Active -> session.launch()
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

	protected fun testSelfUpdate(
		sessionFactory: RemotePackageInstaller.(Uri) -> RemoteSession,
		sessionConfirmation: suspend (RemoteSession) -> Unit
	) = runTest {
		val switchText = if (context.isTv()) {
			ApkFixtures.INSTALLER_LABEL
		} else {
			STANDARD_SWITCH
		}
		ui.launchInstallerApp(
			InstallPermissionRequest.normal(switchText, ApkFixtures.INSTALLER_PACKAGE_NAME)
		)

		var sessionResultDeferred: Deferred<RemoteSession.State>? = null
		val session = AckpineRemoteService
			.bind(
				context,
				ApkFixtures.INSTALLER_PACKAGE_NAME,
				onDisconnected = { sessionResultDeferred?.cancel() }
			)
			.awaitService()
			.packageInstaller
			.sessionFactory(ApkFixtures.installerAppUri())
		val sessionId = session.id

		try {
			sessionResultDeferred = async(start = UNDISPATCHED) { session.awaitWithTimeout() }
			sessionConfirmation(session)
			sessionResultDeferred.await()
		} catch (_: CancellationException) { // ignore
		}

		ui.waitForIdle()
		ui.launchInstallerApp(InstallPermissionRequest.ignore())

		val result = AckpineRemoteService
			.bind(context, ApkFixtures.INSTALLER_PACKAGE_NAME)
			.awaitService()
			.packageInstaller
			.getSession(sessionId)
			?.test()

		ui.forceStopInstallerApp()

		assertEquals(RemoteSession.State.Succeeded, result)
	}
}