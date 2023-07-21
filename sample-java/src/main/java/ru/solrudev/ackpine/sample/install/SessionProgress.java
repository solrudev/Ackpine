package ru.solrudev.ackpine.sample.install;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import ru.solrudev.ackpine.session.Progress;

public final class SessionProgress implements Serializable {

	@NonNull
	private final UUID id;

	private final int progress;
	private final int progressMax;

	public SessionProgress(@NonNull UUID id, @NonNull Progress progress) {
		this.id = id;
		this.progress = progress.getProgress();
		progressMax = progress.getMax();
	}

	@NonNull
	public UUID id() {
		return id;
	}

	@NonNull
	public Progress progress() {
		return new Progress(progress, progressMax);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		SessionProgress that = (SessionProgress) o;
		return progress == that.progress && progressMax == that.progressMax && id.equals(that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, progress, progressMax);
	}

	@NonNull
	@Override
	public String toString() {
		return "SessionProgress{" +
				"id=" + id +
				", progress=" + progress +
				", progressMax=" + progressMax +
				'}';
	}
}