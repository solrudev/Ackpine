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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {

	val uiState = combine(
		settingsRepository.installerBackend,
		settingsRepository.installBestSuitedApks,
		::SettingsUiState
	).stateIn(viewModelScope, SharingStarted.Lazily, SettingsUiState())

	fun selectBackend(backend: InstallerBackend) = viewModelScope.launch {
		settingsRepository.setInstallerBackend(backend)
	}

	fun toggleInstallBestSuitedApks() = viewModelScope.launch {
		settingsRepository.toggleInstallBestSuitedApks()
	}

	companion object {

		val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
			@Suppress("UNCHECKED_CAST")
			override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
				val application = extras[APPLICATION_KEY]!!
				val repository = SettingsRepository(application.preferencesDataStore)
				return SettingsViewModel(repository) as T
			}
		}
	}
}