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

package ru.solrudev.ackpine.sample.install

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import ru.solrudev.ackpine.exceptions.ConflictingBaseApkException
import ru.solrudev.ackpine.exceptions.ConflictingPackageNameException
import ru.solrudev.ackpine.exceptions.ConflictingSplitNameException
import ru.solrudev.ackpine.exceptions.ConflictingVersionCodeException
import ru.solrudev.ackpine.exceptions.NoBaseApkException
import ru.solrudev.ackpine.exceptions.SplitPackageException
import ru.solrudev.ackpine.installer.InstallFailure
import ru.solrudev.ackpine.installer.PackageInstaller
import ru.solrudev.ackpine.installer.createSession
import ru.solrudev.ackpine.installer.getSession
import ru.solrudev.ackpine.resources.ResolvableString
import ru.solrudev.ackpine.sample.R
import ru.solrudev.ackpine.session.ProgressSession
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.session.SessionResult
import ru.solrudev.ackpine.session.await
import ru.solrudev.ackpine.session.progress
import ru.solrudev.ackpine.session.state
import ru.solrudev.ackpine.splits.Apk
import java.util.UUID

class InstallViewModel(
	private val packageInstaller: PackageInstaller,
	private val sessionDataRepository: SessionDataRepository
) : ViewModel() {

	private val error = MutableStateFlow(ResolvableString.empty())

	val uiState = combine(
		error,
		sessionDataRepository.sessions,
		sessionDataRepository.sessionsProgress,
		::InstallUiState
	)
		.onStart { awaitSessionsFromSavedState() }
		.stateIn(viewModelScope, SharingStarted.Lazily, InstallUiState())

	fun installPackage(apks: Sequence<Apk>, fileName: String) = viewModelScope.launch {
		val uris = runInterruptible(Dispatchers.IO) { apks.toUrisList() }
		if (uris.isEmpty()) {
			return@launch
		}
		val session = packageInstaller.createSession(uris) {
			name = fileName
		}
		val sessionData = SessionData(session.id, fileName)
		sessionDataRepository.addSessionData(sessionData)
		awaitSession(session)
	}

	fun cancelSession(id: UUID) = viewModelScope.launch {
		packageInstaller.getSession(id)?.cancel()
	}

	fun removeSession(id: UUID) = sessionDataRepository.removeSessionData(id)

	fun clearError() {
		error.value = ResolvableString.empty()
	}

	private fun awaitSessionsFromSavedState() = viewModelScope.launch {
		val sessions = sessionDataRepository.sessions.value
		if (sessions.isNotEmpty()) {
			sessions
				.map { sessionData ->
					async { packageInstaller.getSession(sessionData.id) }
				}
				.awaitAll()
				.filterNotNull()
				.forEach(::awaitSession)
		}
	}

	private fun awaitSession(session: ProgressSession<InstallFailure>) = viewModelScope.launch {
		session.progress
			.onEach { progress -> sessionDataRepository.updateSessionProgress(session.id, progress) }
			.launchIn(this)
		session.state
			.filterIsInstance<Session.State.Committed>()
			.onEach { sessionDataRepository.updateSessionIsCancellable(session.id, isCancellable = false) }
			.launchIn(this)
		try {
			when (val result = session.await()) {
				is SessionResult.Success -> sessionDataRepository.removeSessionData(session.id)
				is SessionResult.Error -> handleSessionError(result.cause.message, session.id)
			}
		} catch (exception: CancellationException) {
			sessionDataRepository.removeSessionData(session.id)
			throw exception
		} catch (exception: Exception) {
			handleSessionError(exception.message, session.id)
			Log.e("InstallViewModel", null, exception)
		}
	}

	private fun handleSessionError(message: String?, sessionId: UUID) {
		val error = if (message != null) {
			ResolvableString.transientResource(R.string.session_error_with_reason, message)
		} else {
			ResolvableString.transientResource(R.string.session_error)
		}
		sessionDataRepository.setError(sessionId, error)
	}

	private fun Sequence<Apk>.toUrisList(): List<Uri> {
		try {
			return map { it.uri }.toList()
		} catch (exception: SplitPackageException) {
			error.value = when (exception) {
				is NoBaseApkException -> ResolvableString.transientResource(R.string.error_no_base_apk)
				is ConflictingBaseApkException -> ResolvableString.transientResource(R.string.error_conflicting_base_apk)
				is ConflictingSplitNameException -> ResolvableString.transientResource(
					R.string.error_conflicting_split_name,
					exception.name
				)

				is ConflictingPackageNameException -> ResolvableString.transientResource(
					R.string.error_conflicting_package_name,
					exception.expected, exception.actual, exception.name
				)

				is ConflictingVersionCodeException -> ResolvableString.transientResource(
					R.string.error_conflicting_version_code,
					exception.expected, exception.actual, exception.name
				)
			}
			return emptyList()
		} catch (exception: Exception) {
			error.value = ResolvableString.raw(exception.message.orEmpty())
			Log.e("InstallViewModel", null, exception)
			return emptyList()
		}
	}

	companion object {

		val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
			@Suppress("UNCHECKED_CAST")
			override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
				val application = extras[APPLICATION_KEY]!!
				val packageInstaller = PackageInstaller.getInstance(application)
				val savedStateHandle = extras.createSavedStateHandle()
				val sessionsRepository = SessionDataRepositoryImpl(savedStateHandle)
				return InstallViewModel(packageInstaller, sessionsRepository) as T
			}
		}
	}
}