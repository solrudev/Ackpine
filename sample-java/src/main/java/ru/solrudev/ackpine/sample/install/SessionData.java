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

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

import ru.solrudev.ackpine.session.parameters.NotificationString;

public record SessionData(@NonNull UUID id,
						  @NonNull String name,
						  @NonNull NotificationString error,
						  boolean isCancellable) implements Serializable {

	@Serial
	private static final long serialVersionUID = -7412725679599146483L;

	public SessionData(@NonNull UUID id, @NonNull String name) {
		this(id, name, NotificationString.empty(), true);
	}
}