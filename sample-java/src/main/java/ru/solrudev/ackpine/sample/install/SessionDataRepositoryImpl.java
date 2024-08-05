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

package ru.solrudev.ackpine.sample.install;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import ru.solrudev.ackpine.session.Progress;
import ru.solrudev.ackpine.session.parameters.NotificationString;

public final class SessionDataRepositoryImpl implements SessionDataRepository {

	private static final String SESSIONS_KEY = "SESSIONS";
	private static final String SESSIONS_PROGRESS_KEY = "SESSIONS_PROGRESS";
	private final MutableLiveData<List<SessionData>> sessions;
	private final MutableLiveData<List<SessionProgress>> sessionsProgress;

	public SessionDataRepositoryImpl(SavedStateHandle savedStateHandle) {
		sessions = savedStateHandle.getLiveData(SESSIONS_KEY, new ArrayList<>());
		sessionsProgress = savedStateHandle.getLiveData(SESSIONS_PROGRESS_KEY, new ArrayList<>());
	}

	@NonNull
	@Override
	public LiveData<List<SessionData>> getSessions() {
		return sessions;
	}

	@NonNull
	@Override
	public LiveData<List<SessionProgress>> getSessionsProgress() {
		return sessionsProgress;
	}

	@Override
	public void addSessionData(@NonNull SessionData sessionData) {
		final var sessions = getCurrentSessionsCopy();
		sessions.add(sessionData);
		this.sessions.postValue(sessions);
		final var sessionsProgress = getCurrentSessionsProgressCopy();
		sessionsProgress.add(new SessionProgress(sessionData.id(), new Progress()));
		this.sessionsProgress.postValue(sessionsProgress);
	}

	@Override
	public void removeSessionData(@NonNull UUID id) {
		final var sessions = getCurrentSessionsCopy();
		final var sessionDataIndex = getSessionDataIndexById(sessions, id);
		if (sessionDataIndex != -1) {
			sessions.remove(sessionDataIndex);
		}
		this.sessions.setValue(sessions);
		final var sessionsProgress = getCurrentSessionsProgressCopy();
		final var sessionProgressIndex = getSessionProgressIndexById(sessionsProgress, id);
		if (sessionProgressIndex != -1) {
			sessionsProgress.remove(sessionProgressIndex);
		}
		this.sessionsProgress.setValue(sessionsProgress);
	}

	@Override
	public void updateSessionProgress(@NonNull UUID id, @NonNull Progress progress) {
		final var sessionsProgress = getCurrentSessionsProgressCopy();
		final var sessionProgressIndex = getSessionProgressIndexById(sessionsProgress, id);
		if (sessionProgressIndex != -1) {
			sessionsProgress.set(sessionProgressIndex, new SessionProgress(id, progress));
		}
		this.sessionsProgress.setValue(sessionsProgress);
		if (progress.getProgress() <= 80) {
			return;
		}
		final var sessionDataIndex = getSessionDataIndexById(getCurrentSessions(), id);
		if (sessionDataIndex != -1) {
			final var sessionData = getCurrentSessions().get(sessionDataIndex);
			if (!sessionData.isCancellable()) {
				return;
			}
			final var sessions = getCurrentSessionsCopy();
			sessions.set(sessionDataIndex,
					new SessionData(sessionData.id(), sessionData.name(), sessionData.error(), false));
			this.sessions.setValue(sessions);
		}
	}

	@Override
	public void setError(@NonNull UUID id, @NonNull NotificationString error) {
		final var sessions = getCurrentSessionsCopy();
		final var sessionDataIndex = getSessionDataIndexById(sessions, id);
		if (sessionDataIndex != -1) {
			final var sessionData = sessions.get(sessionDataIndex);
			sessions.set(sessionDataIndex,
					new SessionData(sessionData.id(), sessionData.name(), error, sessionData.isCancellable()));
		}
		this.sessions.setValue(sessions);
	}

	@NonNull
	private List<SessionData> getCurrentSessionsCopy() {
		return new ArrayList<>(Objects.requireNonNull(sessions.getValue()));
	}

	@NonNull
	private List<SessionData> getCurrentSessions() {
		return Objects.requireNonNull(sessions.getValue());
	}

	@NonNull
	private List<SessionProgress> getCurrentSessionsProgressCopy() {
		return new ArrayList<>(Objects.requireNonNull(sessionsProgress.getValue()));
	}

	private static int getSessionDataIndexById(@NonNull List<SessionData> sessions, @NonNull UUID id) {
		for (var i = 0; i < sessions.size(); i++) {
			final var session = sessions.get(i);
			if (session.id().equals(id)) {
				return i;
			}
		}
		return -1;
	}

	private static int getSessionProgressIndexById(@NonNull List<SessionProgress> sessions, @NonNull UUID id) {
		for (var i = 0; i < sessions.size(); i++) {
			final var session = sessions.get(i);
			if (session.id().equals(id)) {
				return i;
			}
		}
		return -1;
	}
}