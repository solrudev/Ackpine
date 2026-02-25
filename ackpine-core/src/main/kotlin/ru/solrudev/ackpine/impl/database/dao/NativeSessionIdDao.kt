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

@RestrictTo(RestrictTo.Scope.LIBRARY)
@Dao
internal interface NativeSessionIdDao {

	@Query("INSERT OR REPLACE INTO sessions_native_session_ids(session_id, native_session_id) " +
			"VALUES (:sessionId, :nativeSessionId)")
	fun setNativeSessionId(sessionId: String, nativeSessionId: Int)
}