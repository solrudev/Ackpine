package ru.solrudev.ackpine.impl.database.dao

import androidx.annotation.RestrictTo
import androidx.room.Dao
import androidx.room.Query
import ru.solrudev.ackpine.session.Progress

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Dao
internal interface SessionProgressDao {

	@Query("SELECT progress, max FROM sessions_progress WHERE session_id = :id")
	fun getProgress(id: String): Progress?

	@Query("INSERT OR IGNORE INTO sessions_progress(session_id) VALUES (:id)")
	fun initProgress(id: String)

	@Query(
		"UPDATE sessions_progress SET progress = :progress, max = :max WHERE session_id = :id AND " +
				"EXISTS(SELECT state FROM sessions WHERE id = :id AND state NOT IN ('CANCELLED', 'SUCCEEDED', 'FAILED'))"
	)
	fun updateProgress(id: String, progress: Int, max: Int)
}