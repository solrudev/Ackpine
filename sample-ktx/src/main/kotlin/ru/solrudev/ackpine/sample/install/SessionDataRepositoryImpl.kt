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
		_sessions += sessionData
		_sessionsProgress += SessionProgress(sessionData.id, Progress())
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
		if (progress.progress <= 80) {
			return
		}
		val sessionDataIndex = _sessions.indexOfFirst { it.id == id }
		if (sessionDataIndex != -1) {
			val sessionData = _sessions[sessionDataIndex]
			if (!sessionData.isCancellable) {
				return
			}
			val sessions = _sessions.toMutableList()
			sessions[sessionDataIndex] = sessionData.copy(isCancellable = false)
			_sessions = sessions
		}
	}

	override fun setError(id: UUID, error: NotificationString) {
		val sessions = _sessions.toMutableList()
		val sessionDataIndex = sessions.indexOfFirst { it.id == id }
		if (sessionDataIndex != -1) {
			val sessionData = sessions[sessionDataIndex]
			sessions[sessionDataIndex] = sessionData.copy(error = error)
		}
		_sessions = sessions
	}
}