/*
 * Copyright (C) 2026 Ilya Fomichev
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
import ru.solrudev.ackpine.resources.ResolvableString
import ru.solrudev.ackpine.session.Progress
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class SessionDataRepositoryImplTest {

	@Test
	fun addSessionDataStoresSessionAndProgress() {
		val repository = SessionDataRepositoryImpl(SavedStateHandle())
		val id = UUID.randomUUID()
		val data = SessionData(id, "app.apk")

		repository.addSessionData(data)

		val expectedSessions = listOf(data)
		val actualSessions = repository.sessions.value
		assertEquals(expectedSessions, actualSessions)

		val expectedProgress = listOf(SessionProgress(id, Progress()))
		val actualProgress = repository.sessionsProgress.value
		assertEquals(expectedProgress, actualProgress)
	}

	@Test
	fun removeSessionDataRemovesSessionAndProgress() {
		val repository = SessionDataRepositoryImpl(SavedStateHandle())
		val firstId = UUID.randomUUID()
		val secondId = UUID.randomUUID()
		val firstData = SessionData(firstId, "first.apk")
		val secondData = SessionData(secondId, "second.apk")
		repository.addSessionData(firstData)
		repository.addSessionData(secondData)

		repository.removeSessionData(firstId)

		val expectedSessions = listOf(secondData)
		val actualSessions = repository.sessions.value
		assertEquals(expectedSessions, actualSessions)

		val expectedProgress = listOf(SessionProgress(secondId, Progress()))
		val actualProgress = repository.sessionsProgress.value
		assertEquals(expectedProgress, actualProgress)
	}

	@Test
	fun updateSessionProgressUpdatesMatchingSession() {
		val repository = SessionDataRepositoryImpl(SavedStateHandle())
		val id = UUID.randomUUID()
		repository.addSessionData(SessionData(id, "app.apk"))
		val updated = Progress(progress = 40, max = 100)

		repository.updateSessionProgress(id, updated)

		val actualProgress = repository.sessionsProgress.value.single().progress
		assertEquals(updated, actualProgress)
	}

	@Test
	fun updateSessionIsCancellableUpdatesExistingSession() {
		val repository = SessionDataRepositoryImpl(SavedStateHandle())
		val id = UUID.randomUUID()
		repository.addSessionData(SessionData(id, "app.apk"))

		repository.updateSessionIsCancellable(id, isCancellable = false)

		assertFalse(repository.sessions.value.single().isCancellable)
	}

	@Test
	fun setErrorUpdatesSessionError() {
		val repository = SessionDataRepositoryImpl(SavedStateHandle())
		val id = UUID.randomUUID()
		repository.addSessionData(SessionData(id, "app.apk"))
		val error = ResolvableString.raw("failure")

		repository.setError(id, error)

		val actualError = repository.sessions.value.single().error
		assertEquals(error, actualError)
	}
}