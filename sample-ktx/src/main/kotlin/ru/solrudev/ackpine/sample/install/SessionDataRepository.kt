/*
 * Copyright (C) 2023 Ilya Fomichev
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

import kotlinx.coroutines.flow.StateFlow
import ru.solrudev.ackpine.resources.ResolvableString
import ru.solrudev.ackpine.session.Progress
import java.util.UUID

interface SessionDataRepository {
	val sessions: StateFlow<List<SessionData>>
	val sessionsProgress: StateFlow<List<SessionProgress>>
	fun addSessionData(sessionData: SessionData)
	fun removeSessionData(id: UUID)
	fun updateSessionProgress(id: UUID, progress: Progress)
	fun updateSessionIsCancellable(id: UUID, isCancellable: Boolean)
	fun setError(id: UUID, error: ResolvableString)
}