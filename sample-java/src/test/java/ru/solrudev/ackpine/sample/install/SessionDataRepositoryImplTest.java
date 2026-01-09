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

package ru.solrudev.ackpine.sample.install;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.SavedStateHandle;

import org.junit.Rule;
import org.junit.Test;

import java.util.List;
import java.util.UUID;

import ru.solrudev.ackpine.resources.ResolvableString;
import ru.solrudev.ackpine.session.Progress;

public class SessionDataRepositoryImplTest {

	@Rule
	public final InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

	@Test
	public void addSessionDataStoresSessionAndProgress() {
		final var repository = new SessionDataRepositoryImpl(new SavedStateHandle());
		final var sessionId = UUID.randomUUID();
		final var data = new SessionData(sessionId, "app.apk");

		repository.addSessionData(data);

		final var expectedSessions = List.of(data);
		final var actualSessions = repository.getSessions().getValue();
		assertEquals(expectedSessions, actualSessions);

		final var expectedProgress = List.of(new SessionProgress(sessionId, new Progress()));
		final var actualProgress = repository.getSessionsProgress().getValue();
		assertEquals(expectedProgress, actualProgress);
	}

	@Test
	public void removeSessionDataRemovesSessionAndProgress() {
		final var repository = new SessionDataRepositoryImpl(new SavedStateHandle());
		final var firstId = UUID.randomUUID();
		final var secondId = UUID.randomUUID();
		final var firstData = new SessionData(firstId, "first.apk");
		final var secondData = new SessionData(secondId, "second.apk");
		repository.addSessionData(firstData);
		repository.addSessionData(secondData);

		repository.removeSessionData(firstId);

		final var expectedSessions = List.of(secondData);
		final var actualSessions = repository.getSessions().getValue();
		assertEquals(expectedSessions, actualSessions);

		final var expectedProgress = List.of(new SessionProgress(secondId, new Progress()));
		final var actualProgress = repository.getSessionsProgress().getValue();
		assertEquals(expectedProgress, actualProgress);
	}

	@Test
	public void updateSessionProgressUpdatesMatchingSession() {
		final var repository = new SessionDataRepositoryImpl(new SavedStateHandle());
		final var id = UUID.randomUUID();
		repository.addSessionData(new SessionData(id, "app.apk"));
		final var updated = new Progress(40, 100);

		repository.updateSessionProgress(id, updated);

		final var actualProgress = repository.getSessionsProgress().getValue().getFirst().toProgress();
		assertEquals(updated, actualProgress);
	}

	@Test
	public void updateSessionIsCancellableUpdatesExistingSession() {
		final var repository = new SessionDataRepositoryImpl(new SavedStateHandle());
		final var id = UUID.randomUUID();
		repository.addSessionData(new SessionData(id, "app.apk"));

		repository.updateSessionIsCancellable(id, false);

		assertFalse(repository.getSessions().getValue().getFirst().isCancellable());
	}

	@Test
	public void setErrorUpdatesSessionError() {
		final var repository = new SessionDataRepositoryImpl(new SavedStateHandle());
		final var id = UUID.randomUUID();
		repository.addSessionData(new SessionData(id, "app.apk"));
		final var error = ResolvableString.raw("failure");

		repository.setError(id, error);

		final var sessionError = repository.getSessions().getValue().getFirst().error();
		assertEquals(error, sessionError);
	}
}