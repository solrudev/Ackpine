/*
 * Copyright (C) 2024 Ilya Fomichev
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

package ru.solrudev.ackpine.sample.updater

import android.icu.util.ULocale
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.solrudev.ackpine.AssetFileProvider
import ru.solrudev.ackpine.installer.InstallFailure
import ru.solrudev.ackpine.installer.PackageInstaller
import ru.solrudev.ackpine.installer.createSession
import ru.solrudev.ackpine.installer.getSession
import ru.solrudev.ackpine.installer.parameters.InstallConstraints.TimeoutStrategy
import ru.solrudev.ackpine.installer.parameters.PackageSource
import ru.solrudev.ackpine.installer.parameters.constraints
import ru.solrudev.ackpine.installer.parameters.preapproval
import ru.solrudev.ackpine.resources.ResolvableString
import ru.solrudev.ackpine.session.Progress
import ru.solrudev.ackpine.session.ProgressSession
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.session.await
import ru.solrudev.ackpine.session.parameters.Confirmation
import ru.solrudev.ackpine.session.progress
import ru.solrudev.ackpine.session.state
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.minutes

private const val SESSION_ID_KEY = "SESSION_ID"

private const val ACKPINE_SAMPLE_APP_URL =
	"https://github.com/solrudev/Ackpine/releases/latest/download/sample-ktx-release.apk"

class MainViewModel(
	private val packageInstaller: PackageInstaller,
	private val savedStateHandle: SavedStateHandle
) : ViewModel() {

	private val _uiState = MutableStateFlow(InstallUiState())

	val uiState = _uiState
		.onStart { awaitSessionFromSavedState() }
		.stateIn(viewModelScope, SharingStarted.Lazily, _uiState.value)

	private val isInstalling
		get() = sessionId != null

	private var sessionId: UUID?
		get() = savedStateHandle[SESSION_ID_KEY]
		set(value) {
			savedStateHandle[SESSION_ID_KEY] = value
		}

	fun onButtonClick() {
		if (isInstalling) {
			cancelSession()
			return
		}
		val session = createInstallSession()
		awaitSession(session)
	}

	private fun createInstallSession(): ProgressSession<InstallFailure> {
		val uri = OkHttpProvider.getUriForUrl(ACKPINE_SAMPLE_APP_URL)
		val session = packageInstaller.createSession(uri) {
			name = "Ackpine"
			confirmation = Confirmation.IMMEDIATE
			requestUpdateOwnership = true
			packageSource = PackageSource.Store
			preapproval(
				packageName = "ru.solrudev.ackpine.sample",
				label = "Ackpine",
				locale = ULocale.US
			) {
				icon = AssetFileProvider.getUriForAsset("ackpine_icon.webp")
			}
			constraints(timeout = 1.minutes) {
				timeoutStrategy = TimeoutStrategy.CommitEagerly
				isAppNotInteractingRequired = true
				isAppNotTopVisibleRequired = true
			}
		}
		sessionId = session.id
		return session
	}

	private fun awaitSession(session: ProgressSession<InstallFailure>) {
		session.progress
			.onEach(::updateProgress)
			.launchIn(viewModelScope)
		session.state
			.onEach(::handleSessionState)
			.launchIn(viewModelScope)
		viewModelScope.launch {
			try {
				session.await()
			} catch (exception: CancellationException) {
				throw exception
			} catch (exception: Exception) {
				Log.e("MainViewModel", exception.message, exception)
			} finally {
				savedStateHandle.remove<UUID>(SESSION_ID_KEY)
			}
		}
	}

	private fun handleSessionState(state: Session.State<InstallFailure>) = _uiState.update {
		it.copy(
			error = error(state),
			isInstallationVisible = state is Session.State.Failed || !state.isTerminal,
			isCancellable = state != Session.State.Committed,
			buttonText = buttonText(state)
		)
	}

	private fun updateProgress(progress: Progress) = _uiState.update {
		it.copy(progress = progress)
	}

	private fun cancelSession() = viewModelScope.launch {
		val id = sessionId
		if (id != null) {
			packageInstaller.getSession(id)?.cancel()
		}
	}

	private fun awaitSessionFromSavedState() = viewModelScope.launch {
		val id = sessionId
		if (id != null) {
			packageInstaller.getSession(id)?.let(::awaitSession)
		}
	}

	private fun buttonText(state: Session.State<InstallFailure>) = if (state.isTerminal) {
		ResolvableString.transientResource(R.string.button_install)
	} else {
		ResolvableString.transientResource(R.string.cancel)
	}

	private fun error(state: Session.State<InstallFailure>): ResolvableString {
		val error = if (state is Session.State.Failed) {
			error(state.failure.message)
		} else {
			ResolvableString.empty()
		}
		return error
	}

	private fun error(message: String?) = if (message != null) {
		ResolvableString.transientResource(R.string.error_with_reason, message)
	} else {
		ResolvableString.transientResource(R.string.error)
	}

	companion object {

		val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
			@Suppress("UNCHECKED_CAST")
			override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
				val application = extras[APPLICATION_KEY]!!
				val packageInstaller = PackageInstaller.getInstance(application)
				val savedStateHandle = extras.createSavedStateHandle()
				return MainViewModel(packageInstaller, savedStateHandle) as T
			}
		}
	}
}