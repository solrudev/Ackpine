package ru.solrudev.ackpine.impl.database.dao

import androidx.annotation.RestrictTo
import androidx.room.Dao
import androidx.room.Query

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Dao
internal interface NativeSessionIdDao {

	@Query("SELECT native_session_id FROM sessions_native_session_ids WHERE session_id = :sessionId")
	fun getNativeSessionId(sessionId: String): Int?

	@Query("INSERT OR REPLACE INTO sessions_native_session_ids(session_id, native_session_id) " +
			"VALUES (:sessionId, :nativeSessionId)")
	fun setNativeSessionId(sessionId: String, nativeSessionId: Int)
}