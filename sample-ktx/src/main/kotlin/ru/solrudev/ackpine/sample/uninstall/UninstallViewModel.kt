/*
 * Copyright (C) 2023-2024 Ilya Fomichev
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

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.session.await
import ru.solrudev.ackpine.session.parameters.Confirmation
import ru.solrudev.ackpine.uninstaller.PackageUninstaller
import ru.solrudev.ackpine.uninstaller.UninstallFailure
import ru.solrudev.ackpine.uninstaller.createSession
import ru.solrudev.ackpine.uninstaller.getSession
import java.util.UUID

private const val SESSION_ID_KEY = "SESSION_ID"
private const val PACKAGE_NAME_KEY = "PACKAGE_NAME"

class UninstallViewModel(
	private val packageUninstaller: PackageUninstaller,
	private val savedStateHandle: SavedStateHandle
) : ViewModel() {

	private val _uiState = MutableStateFlow(UninstallUiState())

	val uiState = _uiState
		.onStart { awaitSessionFromSavedState() }
		.stateIn(viewModelScope, SharingStarted.Lazily, UninstallUiState())

	fun loadApplications(refresh: Boolean, applicationsFactory: () -> List<ApplicationData>) {
		if (!refresh && _uiState.value.applications.isNotEmpty()) {
			return
		}
		viewModelScope.launch {
			_uiState.update { it.copy(isLoading = true) }
			val applications = runInterruptible(Dispatchers.Default) { applicationsFactory() }
			_uiState.update { it.copy(isLoading = false, applications = applications) }
		}
	}

	fun uninstallPackage(packageName: String) {
		val session = packageUninstaller.createSession(packageName) {
			confirmation = Confirmation.IMMEDIATE
		}
		savedStateHandle[SESSION_ID_KEY] = session.id
		savedStateHandle[PACKAGE_NAME_KEY] = packageName
		awaitSession(session)
	}

	private fun removeApplication(packageName: String) {
		val applications = _uiState.value.applications.toMutableList()
		applications.removeAll { it.packageName == packageName }
		_uiState.update { it.copy(applications = applications) }
	}

	private fun awaitSessionFromSavedState() = viewModelScope.launch {
		val sessionId = savedStateHandle.get<UUID>(SESSION_ID_KEY)
		if (sessionId != null) {
			packageUninstaller.getSession(sessionId)?.let(::awaitSession)
		}
	}

	private fun clearSavedState() {
		savedStateHandle.remove<UUID>(SESSION_ID_KEY)
		savedStateHandle.remove<String>(PACKAGE_NAME_KEY)
	}

	private fun awaitSession(session: Session<UninstallFailure>) = viewModelScope.launch {
		try {
			when (session.await()) {
				Session.State.Succeeded -> {
					savedStateHandle.get<String>(PACKAGE_NAME_KEY)?.let(::removeApplication)
					clearSavedState()
				}

				is Session.State.Failed -> clearSavedState()
			}
		} catch (exception: CancellationException) {
			throw exception
		} catch (exception: Exception) {
			clearSavedState()
			Log.e("UninstallViewModel", null, exception)
		}
	}

	companion object {

		val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
			@Suppress("UNCHECKED_CAST")
			override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
				val application = extras[APPLICATION_KEY]!!
				val packageUninstaller = PackageUninstaller.getInstance(application)
				val savedStateHandle = extras.createSavedStateHandle()
				return UninstallViewModel(packageUninstaller, savedStateHandle) as T
			}
		}
	}
}