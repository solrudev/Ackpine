package ru.solrudev.ackpine.impl.database.model

import androidx.annotation.RestrictTo
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@RestrictTo(RestrictTo.Scope.LIBRARY)
@Entity(
	tableName = "sessions_notification_ids",
	foreignKeys = [ForeignKey(
		entity = SessionEntity::class,
		parentColumns = ["id"],
		childColumns = ["session_id"],
		onDelete = ForeignKey.CASCADE,
		onUpdate = ForeignKey.CASCADE
	)]
)
internal data class NotificationIdEntity internal constructor(
	@PrimaryKey
	@ColumnInfo(name = "session_id")
	val sessionId: String,
	@ColumnInfo(name = "notification_id")
	val notificationId: Int
)