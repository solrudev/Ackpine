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

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import ru.solrudev.ackpine.session.parameters.NotificationString;

public final class SessionData implements Serializable {

	@NonNull
	private final UUID id;

	@NonNull
	private final String name;

	@NonNull
	private final NotificationString error;

	public SessionData(@NonNull UUID id, @NonNull String name) {
		this.id = id;
		this.name = name;
		error = NotificationString.empty();
	}

	public SessionData(@NonNull UUID id, @NonNull String name, @NonNull NotificationString error) {
		this.id = id;
		this.name = name;
		this.error = error;
	}

	@NonNull
	public UUID id() {
		return id;
	}

	@NonNull
	public String name() {
		return name;
	}

	@NonNull
	public NotificationString error() {
		return error;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		SessionData that = (SessionData) o;
		return id.equals(that.id) && name.equals(that.name) && error.equals(that.error);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, name, error);
	}

	@NonNull
	@Override
	public String toString() {
		return "SessionData{" +
				"id=" + id +
				", name='" + name + '\'' +
				", error=" + error +
				'}';
	}
}