package ru.solrudev.ackpine.sample.install;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import ru.solrudev.ackpine.session.Progress;

public final class SessionProgress implements Serializable {

    @NonNull
    private final UUID _id;

    private final int _progress;
    private final int _progressMax;

    public SessionProgress(@NonNull UUID id, @NonNull Progress progress) {
        _id = id;
        _progress = progress.getProgress();
        _progressMax = progress.getMax();
    }

    @NonNull
    public UUID id() {
        return _id;
    }

    @NonNull
    public Progress progress() {
        return new Progress(_progress, _progressMax);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SessionProgress that = (SessionProgress) o;
        return _progress == that._progress && _progressMax == that._progressMax && _id.equals(that._id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_id, _progress, _progressMax);
    }

    @NonNull
    @Override
    public String toString() {
        return "SessionProgress{" +
                "id=" + _id +
                ", progress=" + _progress +
                ", progressMax=" + _progressMax +
                '}';
    }
}