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
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ru.solrudev.ackpine.impl.database.model.SessionEntity

@RestrictTo(RestrictTo.Scope.LIBRARY)
@Dao
internal interface SessionDao {

	@Insert(onConflict = OnConflictStrategy.IGNORE)
	fun insertSession(session: SessionEntity)

	@Query("UPDATE sessions SET state = :state WHERE id = :id")
	fun updateSessionState(id: String, state: SessionEntity.State)

	@Query("UPDATE sessions SET last_launch_timestamp = :lastLaunchTimestamp WHERE id = :id")
	fun updateLastLaunchTimestamp(id: String, lastLaunchTimestamp: Long)

	@Query("UPDATE sessions SET last_commit_timestamp = :lastCommitTimestamp WHERE id = :id")
	fun updateLastCommitTimestamp(id: String, lastCommitTimestamp: Long)
}