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

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.updateAndGet

suspend fun createSettingsRepository(
	backend: InstallerBackend = InstallerBackend.ROOTLESS,
	supportsShizuku: Flow<Boolean> = flowOf(false)
): SettingsRepository {
	val dataStore = TestPreferencesDataStore()
	val repository = SettingsRepository(dataStore, supportsShizuku)
	repository.setInstallerBackend(backend)
	return repository
}

private class TestPreferencesDataStore(
	initialPreferences: Preferences = emptyPreferences()
) : DataStore<Preferences> {

	override val data = MutableStateFlow(initialPreferences)

	override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
		return data.updateAndGet { transform(it) }
	}
}