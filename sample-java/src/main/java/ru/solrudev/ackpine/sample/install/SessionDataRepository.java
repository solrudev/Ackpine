package ru.solrudev.ackpine.sample.install;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.UUID;

import ru.solrudev.ackpine.session.Progress;
import ru.solrudev.ackpine.session.parameters.NotificationString;

public interface SessionDataRepository {

    @NonNull
    LiveData<List<SessionData>> getSessions();

    @NonNull
    LiveData<List<SessionProgress>> getSessionsProgress();

    void addSessionData(@NonNull SessionData sessionData);

    void removeSessionData(@NonNull UUID id);

    void updateSessionProgress(@NonNull UUID id, @NonNull Progress progress);

    void setError(@NonNull UUID id, @NonNull NotificationString error);
}
