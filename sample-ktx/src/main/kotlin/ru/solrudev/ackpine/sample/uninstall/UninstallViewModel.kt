package ru.solrudev.ackpine.sample.uninstall

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.session.SessionResult
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

	init {
		viewModelScope.launch {
			val sessionId = savedStateHandle.get<UUID>(SESSION_ID_KEY)
			if (sessionId != null) {
				packageUninstaller.getSession(sessionId)?.let(::awaitSession)
			}
		}
	}

	private val _uiState = MutableStateFlow(UninstallUiState())
	val uiState = _uiState.asStateFlow()

	fun loadApplications(refresh: Boolean, applicationsFactory: () -> List<ApplicationData>) {
		if (!refresh && _uiState.value.applications.isNotEmpty()) {
			return
		}
		viewModelScope.launch {
			_uiState.update { it.copy(isLoading = true) }
			val applications = runInterruptible(Dispatchers.Default) { applicationsFactory() }
			_uiState.update {
				it.copy(isLoading = false, applications = applications)
			}
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

	private fun clearSavedState() {
		savedStateHandle.remove<UUID>(SESSION_ID_KEY)
		savedStateHandle.remove<String>(PACKAGE_NAME_KEY)
	}

	private fun awaitSession(session: Session<UninstallFailure>) = viewModelScope.launch {
		when (session.await()) {
			is SessionResult.Success -> {
				savedStateHandle.get<String>(PACKAGE_NAME_KEY)?.let(::removeApplication)
				clearSavedState()
			}
			is SessionResult.Error -> clearSavedState()
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