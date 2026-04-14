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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryTest {

	@Test
	fun installerBackendDefaultsToRootless() = runTest {
		val repository = createSettingsRepository()
		assertEquals(InstallerBackend.ROOTLESS, repository.installerBackend.first())
	}

	@Test
	fun installerBackendPersistsSelection() = runTest {
		val repository = createSettingsRepository(supportsShizuku = flowOf(true))
		repository.setInstallerBackend(InstallerBackend.SHIZUKU)
		assertEquals(InstallerBackend.SHIZUKU, repository.installerBackend.first())
	}

	@Test
	fun installerBackendFallsBackToRootlessWhenShizukuBecomesUnavailable() = runTest {
		val supportsShizuku = MutableStateFlow(true)
		val repository = createSettingsRepository(InstallerBackend.SHIZUKU, supportsShizuku)
		repository.installerBackend.test {
			assertEquals(InstallerBackend.SHIZUKU, awaitItem())

			supportsShizuku.value = false
			advanceUntilIdle()

			assertEquals(InstallerBackend.ROOTLESS, awaitItem())
		}
	}

	@Test
	fun installBestSuitedApksDefaultsToTrue() = runTest {
		val repository = createSettingsRepository()
		assertTrue(repository.installBestSuitedApks.first())
	}

	@Test
	fun toggleInstallBestSuitedApksFlipsValue() = runTest {
		val repository = createSettingsRepository()
		repository.toggleInstallBestSuitedApks()
		assertFalse(repository.installBestSuitedApks.first())
	}

	@Test
	fun toggleInstallBestSuitedApksTwiceRestoresValue() = runTest {
		val repository = createSettingsRepository()
		repository.toggleInstallBestSuitedApks()
		repository.toggleInstallBestSuitedApks()
		assertTrue(repository.installBestSuitedApks.first())
	}

	@Test
	fun shizukuSupportFlowReflectsBinderAvailabilityChanges() = runTest {
		val supportsShizuku = MutableStateFlow(true)
		val repository = createSettingsRepository(InstallerBackend.SHIZUKU, supportsShizuku)
		repository.isShizukuAvailable.test {
			assertTrue(awaitItem())

			supportsShizuku.value = false
			advanceUntilIdle()

			assertFalse(awaitItem())
		}
	}
}