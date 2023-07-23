package ru.solrudev.ackpine.sample.install

import kotlinx.coroutines.flow.StateFlow
import ru.solrudev.ackpine.session.Progress
import ru.solrudev.ackpine.session.parameters.NotificationString
import java.util.UUID

interface SessionDataRepository {
	val sessions: StateFlow<List<SessionData>>
	val sessionsProgress: StateFlow<List<SessionProgress>>
	fun addSessionData(sessionData: SessionData)
	fun removeSessionData(id: UUID)
	fun updateSessionProgress(id: UUID, progress: Progress)
	fun setError(id: UUID, error: NotificationString)
}