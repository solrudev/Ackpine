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

package ru.solrudev.ackpine.sample.settings

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import ru.solrudev.ackpine.sample.MainDispatcherRule
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

	@JvmField
	@Rule
	val mainDispatcherRule = MainDispatcherRule()

	@Test
	fun uiStateDefaultsToRootlessWithoutShizukuSupport() = runTest(mainDispatcherRule.dispatcher) {
		val repository = createSettingsRepository()
		val viewModel = SettingsViewModel(repository)
		assertEquals(SettingsUiState(), viewModel.uiState.value)
	}

	@Test
	fun uiStateReflectsBackendSelectionAndShizukuSupport() = runTest(mainDispatcherRule.dispatcher) {
		val supportsShizuku = MutableStateFlow(true)
		val repository = createSettingsRepository(supportsShizuku = supportsShizuku)
		val viewModel = SettingsViewModel(repository)

		viewModel.uiState.test {
			assertEquals(SettingsUiState(installerBackend = InstallerBackend.ROOTLESS), awaitItem())

			viewModel.selectBackend(InstallerBackend.SHIZUKU)
			advanceUntilIdle()

			assertEquals(SettingsUiState(installerBackend = InstallerBackend.SHIZUKU), awaitItem())

			supportsShizuku.value = false
			advanceUntilIdle()

			assertEquals(SettingsUiState(), awaitItem())
		}
	}
}