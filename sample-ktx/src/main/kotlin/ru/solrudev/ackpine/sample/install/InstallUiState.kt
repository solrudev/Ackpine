package ru.solrudev.ackpine.sample.install

import ru.solrudev.ackpine.session.parameters.NotificationString

data class InstallUiState(
	val error: NotificationString = NotificationString.empty(),
	val sessions: List<SessionData> = emptyList(),
	val sessionsProgress: List<SessionProgress> = emptyList()
)