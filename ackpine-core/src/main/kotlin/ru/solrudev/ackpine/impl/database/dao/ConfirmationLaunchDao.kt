/*
 * Copyright (C) 2025 Ilya Fomichev
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

package ru.solrudev.ackpine.impl.database.dao

import androidx.annotation.RestrictTo
import androidx.room.Dao
import androidx.room.Query

@RestrictTo(RestrictTo.Scope.LIBRARY)
@Dao
internal interface ConfirmationLaunchDao {

	@Query(
		"INSERT OR IGNORE INTO sessions_confirmation_launches(session_id, was_confirmation_launched) " +
				"VALUES (:sessionId, 1)"
	)
	fun setConfirmationLaunched(sessionId: String)
}