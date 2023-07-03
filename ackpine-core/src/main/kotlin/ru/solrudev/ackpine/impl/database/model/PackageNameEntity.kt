package ru.solrudev.ackpine.impl.database.model

import androidx.annotation.RestrictTo
import androidx.room.*

@RestrictTo(RestrictTo.Scope.LIBRARY)
@Entity(
	tableName = "sessions_package_names",
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
	@ColumnInfo(name = "session_id", index = true)
	val sessionId: String,
	@ColumnInfo(name = "package_name")
	val packageName: String
)