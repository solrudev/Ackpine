package ru.solrudev.ackpine.sample.install;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import ru.solrudev.ackpine.session.parameters.NotificationString;

public final class SessionData implements Serializable {

    @NonNull
    private final UUID _id;

    @NonNull
    private final String _name;

    @NonNull
    private final NotificationString _error;

    public SessionData(@NonNull UUID id, @NonNull String name) {
        _id = id;
        _name = name;
        _error = NotificationString.empty();
    }

    public SessionData(@NonNull UUID id, @NonNull String name, @NonNull NotificationString error) {
        _id = id;
        _name = name;
        _error = error;
    }

    @NonNull
    public UUID id() {
        return _id;
    }

    @NonNull
    public String name() {
        return _name;
    }

    @NonNull
    public NotificationString error() {
        return _error;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SessionData that = (SessionData) o;
        return _id.equals(that._id) && _name.equals(that._name) && _error.equals(that._error);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_id, _name, _error);
    }

    @NonNull
    @Override
    public String toString() {
        return "SessionData{" +
                "id=" + _id +
                ", name='" + _name + '\'' +
                ", error=" + _error +
                '}';
    }
}