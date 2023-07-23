package ru.solrudev.ackpine.sample.uninstall

data class UninstallUiState(
	val isLoading: Boolean = false,
	val applications: List<ApplicationData> = emptyList()
)