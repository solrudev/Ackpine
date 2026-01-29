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

package ru.solrudev.ackpine.impl

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import ru.solrudev.ackpine.impl.InstallPermissionRequest.Companion.STANDARD_SWITCH
import ru.solrudev.ackpine.impl.testutil.UiAutomationUtility
import ru.solrudev.ackpine.impl.testutil.isAndroid11
import ru.solrudev.ackpine.impl.testutil.isTv
import ru.solrudev.ackpine.impl.testutil.test
import ru.solrudev.ackpine.impl.uninstaller.activity.isPackageInstalled
import ru.solrudev.ackpine.installer.PackageInstaller
import ru.solrudev.ackpine.installer.createSession
import ru.solrudev.ackpine.installer.parameters.InstallerType
import ru.solrudev.ackpine.remote.AckpineRemoteService
import ru.solrudev.ackpine.remote.RemoteSession
import ru.solrudev.ackpine.remote.state
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.session.parameters.Confirmation
import ru.solrudev.ackpine.uninstaller.PackageUninstaller
import ru.solrudev.ackpine.uninstaller.createSession
import ru.solrudev.ackpine.uninstaller.parameters.UninstallerType
import java.util.UUID
import kotlin.test.assertEquals

open class AckpineTest(
	allowUnknownSources: Boolean = !isAndroid11()
) {

	@Rule
	@JvmField
	val permissionRule = AckpineTestRule(allowUnknownSources)

	protected val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
	protected val installer = PackageInstaller.getInstance(context)
	protected val uninstaller = PackageUninstaller.getInstance(context)
	protected val ui = UiAutomationUtility()

	/**
	 * Test case for system-initiated process death.
	 *
	 * Cancel the job passed into [stateHandler] when you want the process to be killed.
	 */
	protected fun testProcessDeathRecovery(
		sessionFactory: AckpineRemoteService.() -> RemoteSession,
		stateHandler: suspend (RemoteSession, RemoteSession.State, Job?) -> Unit,
		sessionConfirmation: suspend (RemoteSession) -> Unit,
		getSession: AckpineRemoteService.(UUID) -> RemoteSession?,
	) = runTest {
		val switchText = if (context.isTv()) {
			ApkFixtures.INSTALLER_LABEL
		} else {
			STANDARD_SWITCH
		}
		ui.launchInstallerApp(
			InstallPermissionRequest.normal(switchText, ApkFixtures.INSTALLER_PACKAGE_NAME)
		)
		val sessionId: UUID

		AckpineRemoteService.use(context, ApkFixtures.INSTALLER_PACKAGE_NAME) { connection ->
			val session = connection
				.awaitService()
				.sessionFactory()
			sessionId = session.id
			var job: Job? = null
			job = session.state
				.onEach { state -> stateHandler(session, state, job) }
				.launchIn(this)
			job.join()
		}

		ui.hideAndKillInstallerApp()
		ui.waitForIdle()
		ui.openRecentInstallerApp()

		val result = AckpineRemoteService
			.bind(context, ApkFixtures.INSTALLER_PACKAGE_NAME)
			.awaitService()
			.getSession(sessionId)
			?.test(block = sessionConfirmation)

		ui.forceStopInstallerApp()

		assertEquals(RemoteSession.State.Succeeded, result)
	}

	protected suspend fun uninstallFixtureIfPresent() = coroutineScope {
		if (!context.isPackageInstalled(ApkFixtures.FIXTURE_PACKAGE_NAME)) {
			return@coroutineScope
		}
		if (isAndroid11()) {
			ui.launchInstallerApp(
				InstallPermissionRequest.normal(STANDARD_SWITCH, ApkFixtures.INSTALLER_PACKAGE_NAME)
			)
			val result = AckpineRemoteService.use(context, ApkFixtures.INSTALLER_PACKAGE_NAME) { connection ->
				connection
					.awaitService()
					.packageUninstaller
					.createImmediateSession(UninstallerType.DEFAULT, ApkFixtures.FIXTURE_PACKAGE_NAME)
					.test { ui.clickOk() }
			}
			assertEquals(RemoteSession.State.Succeeded, result)
			ui.forceStopInstallerApp()
			ui.waitForIdle()
			return@coroutineScope
		}
		val session = uninstaller.createSession(ApkFixtures.FIXTURE_PACKAGE_NAME) {
			confirmation = Confirmation.IMMEDIATE
		}
		val result = session.test { ui.clickOk() }
		assertEquals(Session.State.Succeeded, result)
		ui.waitForIdle()
	}

	protected suspend fun installFixtureIfAbsent() = coroutineScope {
		if (context.isPackageInstalled(ApkFixtures.FIXTURE_PACKAGE_NAME)) {
			return@coroutineScope
		}
		if (isAndroid11()) {
			ui.launchInstallerApp(
				InstallPermissionRequest.normal(STANDARD_SWITCH, ApkFixtures.INSTALLER_PACKAGE_NAME)
			)
			val result = AckpineRemoteService.use(context, ApkFixtures.INSTALLER_PACKAGE_NAME) { connection ->
				connection
					.awaitService()
					.packageInstaller
					.createImmediateSession(InstallerType.DEFAULT, ApkFixtures.fixtureUri())
					.test { ui.clickInstallOrUpdate() }
			}
			assertEquals(RemoteSession.State.Succeeded, result)
			ui.forceStopInstallerApp()
			ui.waitForIdle()
			return@coroutineScope
		}
		val session = installer.createSession(ApkFixtures.fixtureUri()) {
			confirmation = Confirmation.IMMEDIATE
		}
		val result = session.test { ui.clickInstallOrUpdate() }
		assertEquals(Session.State.Succeeded, result)
		ui.waitForIdle()
	}
}