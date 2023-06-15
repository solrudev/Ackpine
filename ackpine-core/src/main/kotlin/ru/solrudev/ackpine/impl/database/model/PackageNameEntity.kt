package ru.solrudev.ackpine.impl.database.model

import androidx.annotation.RestrictTo
import androidx.room.*

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Entity(
	tableName = "sessions_package_names",
	indices = [Index("session_id")],
	foreignKeys = [ForeignKey(
		entity = SessionEntity::class,
		parentColumns = ["id"],
		childColumns = ["session_id"],
		onDelete = ForeignKey.CASCADE,
		onUpdate = ForeignKey.CASCADE
	)]
)
internal data class PackageNameEntity internal constructor(
	@PrimaryKey(autoGenerate = true)
	@ColumnInfo(name = "id")
	val id: Int = 0,
	@ColumnInfo(name = "session_id")
	val sessionId: String,
	@ColumnInfo(name = "package_name")
	val packageName: String
)