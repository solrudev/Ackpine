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

	@Query("DELETE FROM sessions WHERE id = :id")
	fun deleteSession(id: String)

	@Query("SELECT * FROM sessions WHERE id = :id")
	fun getSession(id: String): SessionEntity?

	@Query("SELECT state FROM sessions WHERE id = :id")
	fun getSessionState(id: String): SessionEntity.State?

	@Query("UPDATE sessions SET state = :state WHERE id = :id")
	fun updateSessionState(id: String, state: SessionEntity.State)
}