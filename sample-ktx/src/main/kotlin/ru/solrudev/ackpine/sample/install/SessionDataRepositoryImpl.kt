package ru.solrudev.ackpine.sample.install

import androidx.lifecycle.SavedStateHandle
import ru.solrudev.ackpine.session.Progress
import ru.solrudev.ackpine.session.parameters.NotificationString
import java.util.UUID

private const val SESSIONS_KEY = "SESSIONS"
private const val SESSIONS_PROGRESS_KEY = "SESSIONS_PROGRESS"

class SessionDataRepositoryImpl(private val savedStateHandle: SavedStateHandle) : SessionDataRepository {

	private var _sessions: List<SessionData>
		get() = savedStateHandle[SESSIONS_KEY] ?: emptyList()
		set(value) {
			savedStateHandle[SESSIONS_KEY] = value
		}

	private var _sessionsProgress: List<SessionProgress>
		get() = savedStateHandle[SESSIONS_PROGRESS_KEY] ?: emptyList()
		set(value) {
			savedStateHandle[SESSIONS_PROGRESS_KEY] = value
		}

	override val sessions = savedStateHandle.getStateFlow<List<SessionData>>(
		SESSIONS_KEY, emptyList()
	)

	override val sessionsProgress = savedStateHandle.getStateFlow<List<SessionProgress>>(
		SESSIONS_PROGRESS_KEY, emptyList()
	)

	override fun addSessionData(sessionData: SessionData) {
		val sessions = _sessions.toMutableList()
		sessions.add(sessionData)
		_sessions = sessions
		val sessionsProgress = _sessionsProgress.toMutableList()
		sessionsProgress.add(SessionProgress(sessionData.id, Progress()))
		_sessionsProgress = sessionsProgress
	}

	override fun removeSessionData(id: UUID) {
		val sessions = _sessions.toMutableList()
		sessions.removeAll { it.id == id }
		_sessions = sessions
		val sessionsProgress = _sessionsProgress.toMutableList()
		sessionsProgress.removeAll { it.id == id }
		_sessionsProgress = sessionsProgress
	}

	override fun updateSessionProgress(id: UUID, progress: Progress) {
		val sessionsProgress = _sessionsProgress.toMutableList()
		val sessionProgressIndex = sessionsProgress.indexOfFirst { it.id == id }
		if (sessionProgressIndex != -1) {
			sessionsProgress[sessionProgressIndex] = SessionProgress(id, progress)
		}
		_sessionsProgress = sessionsProgress
	}

	override fun setError(id: UUID, error: NotificationString) {
		val sessions = _sessions.toMutableList()
		val sessionDataIndex = sessions.indexOfFirst { it.id == id }
		if (sessionDataIndex != -1) {
			val sessionData = sessions[sessionDataIndex]
			sessions[sessionDataIndex] = SessionData(sessionData.id, sessionData.name, error)
		}
		_sessions = sessions
	}
}