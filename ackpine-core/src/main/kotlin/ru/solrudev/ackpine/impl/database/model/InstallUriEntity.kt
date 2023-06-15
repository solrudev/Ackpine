package ru.solrudev.ackpine.impl.database.model

import androidx.annotation.RestrictTo
import androidx.room.*

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Entity(
	tableName = "sessions_install_uris",
	indices = [Index("session_id")],
	foreignKeys = [ForeignKey(
		entity = SessionEntity::class,
		parentColumns = ["id"],
		childColumns = ["session_id"],
		onDelete = ForeignKey.CASCADE,
		onUpdate = ForeignKey.CASCADE
	)]
)
internal data class InstallUriEntity internal constructor(
	@PrimaryKey(autoGenerate = true)
	@ColumnInfo(name = "id")
	val id: Int = 0,
	@ColumnInfo(name = "session_id")
	val sessionId: String,
	@ColumnInfo(name = "uri")
	val uri: String
)