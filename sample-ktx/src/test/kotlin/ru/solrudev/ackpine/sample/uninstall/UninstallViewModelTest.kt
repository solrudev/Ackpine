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

package ru.solrudev.ackpine.sample.uninstall

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import ru.solrudev.ackpine.sample.testing.MainDispatcherRule
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.test.TestSessionScript
import ru.solrudev.ackpine.test.TestUninstallSession
import ru.solrudev.ackpine.test.TestPackageUninstaller
import ru.solrudev.ackpine.uninstaller.UninstallFailure
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private const val SESSION_ID_KEY = "SESSION_ID"
private const val PACKAGE_NAME_KEY = "PACKAGE_NAME"
private const val PACKAGE_NAME = "com.example.app"

@OptIn(ExperimentalCoroutinesApi::class)
class UninstallViewModelTest {

	@JvmField
	@Rule
	val mainDispatcherRule = MainDispatcherRule()

	@Test
	fun loadApplicationsPopulatesUiState() = runTest(mainDispatcherRule.dispatcher) {
		val viewModel = UninstallViewModel(TestPackageUninstaller(), SavedStateHandle(), coroutineContext)
		val app = createApplicationData()
		viewModel.uiState.test {
			awaitItem() // initial state

			viewModel.loadApplications(refresh = true) { listOf(app) }

			val expectedState1 = UninstallUiState(isLoading = true)
			assertEquals(expectedState1, awaitItem())

			val expectedState2 = UninstallUiState(
				isLoading = false,
				applications = listOf(app)
			)
			assertEquals(expectedState2, awaitItem())
		}
	}

	@Test
	fun uninstallPackageSuccessfulFlow() = runTest(mainDispatcherRule.dispatcher) {
		val uninstaller = TestPackageUninstaller()
		val savedStateHandle = SavedStateHandle()
		val viewModel = UninstallViewModel(uninstaller, savedStateHandle, coroutineContext)
		val app = createApplicationData()
		viewModel.uiState.test {
			awaitItem() // initial state
			viewModel.loadApplications(refresh = true) { listOf(app) }
			awaitItem() // loading apps
			awaitItem() // apps loaded

			viewModel.uninstallPackage(app.packageName)
			assertEquals(uninstaller.sessions.last().id, savedStateHandle[SESSION_ID_KEY])
			assertEquals(app.packageName, savedStateHandle[PACKAGE_NAME_KEY])

			assertEquals(UninstallUiState(), awaitItem())
			assertNull(savedStateHandle[SESSION_ID_KEY])
			assertNull(savedStateHandle[PACKAGE_NAME_KEY])
		}
	}

	@Test
	fun uninstallPackageFailureFlow() = runTest(mainDispatcherRule.dispatcher) {
		val script: TestSessionScript<UninstallFailure> = TestSessionScript.auto(
			Session.State.Failed(UninstallFailure.Generic("Failure"))
		)
		val uninstaller = TestPackageUninstaller(script)
		val savedStateHandle = SavedStateHandle()
		val viewModel = UninstallViewModel(uninstaller, savedStateHandle, coroutineContext)
		val app = createApplicationData()
		viewModel.uiState.test {
			awaitItem() // initial state
			viewModel.loadApplications(refresh = true) { listOf(app) }
			awaitItem() // loading apps
			awaitItem() // apps loaded

			viewModel.uninstallPackage(app.packageName)

			val expectedState = UninstallUiState(
				isLoading = false,
				applications = listOf(app),
				failure = "Failure"
			)
			assertEquals(expectedState, awaitItem())
			assertNull(savedStateHandle[SESSION_ID_KEY])
			assertNull(savedStateHandle[PACKAGE_NAME_KEY])

			viewModel.clearFailure()
			assertEquals(expectedState.copy(failure = null), awaitItem())
		}
	}

	@Test
	fun restoresSessionFromSavedStateAndClearsOnCompletion() = runTest(mainDispatcherRule.dispatcher) {
		val script: TestSessionScript<UninstallFailure> = TestSessionScript.auto(Session.State.Succeeded)
		val sessionId = UUID.randomUUID()
		val session = TestUninstallSession(script, sessionId)
		val uninstaller = TestPackageUninstaller()
		uninstaller.seedSession(session)
		val savedStateHandle = SavedStateHandle(
			mapOf(
				SESSION_ID_KEY to sessionId,
				PACKAGE_NAME_KEY to PACKAGE_NAME
			)
		)
		val viewModel = UninstallViewModel(uninstaller, savedStateHandle, coroutineContext)
		viewModel.uiState.first() // trigger collection
		advanceUntilIdle()

		assertNull(savedStateHandle[SESSION_ID_KEY])
		assertNull(savedStateHandle[PACKAGE_NAME_KEY])
	}

	private fun createApplicationData() = ApplicationData("App", PACKAGE_NAME, TestDrawable())
}