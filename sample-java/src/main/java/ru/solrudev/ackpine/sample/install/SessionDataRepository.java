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

import java.util.List;
import java.util.UUID;

import ru.solrudev.ackpine.resources.ResolvableString;
import ru.solrudev.ackpine.session.Progress;

public interface SessionDataRepository {

	@NonNull
	LiveData<List<SessionData>> getSessions();

	@NonNull
	LiveData<List<SessionProgress>> getSessionsProgress();

	void addSessionData(@NonNull SessionData sessionData);

	void removeSessionData(@NonNull UUID id);

	void updateSessionProgress(@NonNull UUID id, @NonNull Progress progress);

	void updateSessionIsCancellable(@NonNull UUID id, boolean isCancellable);

	void setError(@NonNull UUID id, @NonNull ResolvableString error);
}
