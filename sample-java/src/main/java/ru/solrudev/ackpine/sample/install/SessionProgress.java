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

package ru.solrudev.ackpine.sample.install;

import androidx.annotation.NonNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import ru.solrudev.ackpine.session.Progress;

public final class SessionProgress implements Serializable {

	@Serial
	private static final long serialVersionUID = -1923187412469582409L;

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