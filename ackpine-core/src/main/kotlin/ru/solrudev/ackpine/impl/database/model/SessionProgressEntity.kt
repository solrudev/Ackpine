package ru.solrudev.ackpine.impl.database.model

import androidx.annotation.RestrictTo
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Entity(
	tableName = "sessions_progress",
	foreignKeys = [ForeignKey(
		entity = SessionEntity::class,
		parentColumns = ["id"],
		childColumns = ["session_id"],
		onDelete = ForeignKey.CASCADE,
		onUpdate = ForeignKey.CASCADE
	)]
)
internal data class SessionProgressEntity internal constructor(
	@PrimaryKey
	@ColumnInfo(name = "session_id")
	val sessionId: String,
	@ColumnInfo(name = "progress", defaultValue = "0")
	val progress: Int,
	@ColumnInfo(name = "max", defaultValue = "100")
	val max: Int
)