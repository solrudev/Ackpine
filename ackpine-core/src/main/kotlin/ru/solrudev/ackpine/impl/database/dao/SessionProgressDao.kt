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

package ru.solrudev.ackpine.impl.database.dao

import androidx.annotation.RestrictTo
import androidx.room.Dao
import androidx.room.Query
import ru.solrudev.ackpine.impl.database.model.SessionEntity.State.Companion.TERMINAL_STATES
import ru.solrudev.ackpine.session.Progress

@RestrictTo(RestrictTo.Scope.LIBRARY)
@Dao
internal interface SessionProgressDao {

	@Query("SELECT progress, max FROM sessions_progress WHERE session_id = :id")
	fun getProgress(id: String): Progress?

	@Query("INSERT OR IGNORE INTO sessions_progress(session_id) VALUES (:id)")
	fun initProgress(id: String)

	@Query(
		"UPDATE sessions_progress SET progress = :progress, max = :max WHERE session_id = :id AND " +
				"EXISTS(SELECT state FROM sessions WHERE id = :id AND state NOT IN $TERMINAL_STATES)"
	)
	fun updateProgress(id: String, progress: Int, max: Int)
}