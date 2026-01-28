/*
 * Copyright (C) 2024 Ilya Fomichev
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
import androidx.room.Transaction
import ru.solrudev.ackpine.impl.database.AckpineDatabase

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal interface LastUpdateTimestampDao {
	fun setLastUpdateTimestamp(sessionId: String, packageName: String, lastUpdateTimestamp: Long)
	fun setLastUpdateTimestamp(sessionId: String, lastUpdateTimestamp: Long)
}

@RestrictTo(RestrictTo.Scope.LIBRARY)
@Dao
internal abstract class LastUpdateTimestampDaoImpl protected constructor(
	private val database: AckpineDatabase
) : LastUpdateTimestampDao {

	@Transaction
	override fun setLastUpdateTimestamp(sessionId: String, packageName: String, lastUpdateTimestamp: Long) {
		database.installSessionDao().insertPackageName(sessionId, packageName)
		setLastUpdateTimestamp(sessionId, lastUpdateTimestamp)
	}

	@Query("INSERT OR REPLACE INTO sessions_last_install_timestamps(session_id, last_update_timestamp) " +
				"VALUES (:sessionId, :lastUpdateTimestamp)")
	abstract override fun setLastUpdateTimestamp(sessionId: String, lastUpdateTimestamp: Long)
}