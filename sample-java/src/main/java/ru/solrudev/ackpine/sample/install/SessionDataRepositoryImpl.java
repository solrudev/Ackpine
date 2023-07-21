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
		final var sessions = getCurrentSessions();
		sessions.add(sessionData);
		this.sessions.postValue(sessions);
		final var sessionsProgress = getCurrentSessionsProgress();
		sessionsProgress.add(new SessionProgress(sessionData.id(), new Progress()));
		this.sessionsProgress.postValue(sessionsProgress);
	}

	@Override
	public void removeSessionData(@NonNull UUID id) {
		final var sessions = getCurrentSessions();
		final var sessionDataIndex = getSessionDataIndexById(sessions, id);
		if (sessionDataIndex != -1) {
			sessions.remove(sessionDataIndex);
		}
		this.sessions.setValue(sessions);
		final var sessionsProgress = getCurrentSessionsProgress();
		final var sessionProgressIndex = getSessionProgressIndexById(sessionsProgress, id);
		if (sessionProgressIndex != -1) {
			sessionsProgress.remove(sessionProgressIndex);
		}
		this.sessionsProgress.setValue(sessionsProgress);
	}

	@Override
	public void updateSessionProgress(@NonNull UUID id, @NonNull Progress progress) {
		final var sessionsProgress = getCurrentSessionsProgress();
		final var sessionProgressIndex = getSessionProgressIndexById(sessionsProgress, id);
		if (sessionProgressIndex != -1) {
			sessionsProgress.set(sessionProgressIndex, new SessionProgress(id, progress));
		}
		this.sessionsProgress.setValue(sessionsProgress);
	}

	@Override
	public void setError(@NonNull UUID id, @NonNull NotificationString error) {
		final var sessions = getCurrentSessions();
		final var sessionDataIndex = getSessionDataIndexById(sessions, id);
		if (sessionDataIndex != -1) {
			final var sessionData = sessions.get(sessionDataIndex);
			sessions.set(sessionDataIndex, new SessionData(sessionData.id(), sessionData.name(), error));
		}
		this.sessions.setValue(sessions);
	}

	@NonNull
	private List<SessionData> getCurrentSessions() {
		return new ArrayList<>(Objects.requireNonNull(sessions.getValue()));
	}

	@NonNull
	private List<SessionProgress> getCurrentSessionsProgress() {
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