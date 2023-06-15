package ru.solrudev.ackpine.impl.database.model

import androidx.annotation.RestrictTo
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import ru.solrudev.ackpine.installer.InstallFailure

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Entity(
	tableName = "sessions_install_failures",
	foreignKeys = [ForeignKey(
		entity = SessionEntity::class,
		parentColumns = ["id"],
		childColumns = ["session_id"],
		onDelete = ForeignKey.CASCADE,
		onUpdate = ForeignKey.CASCADE
	)]
)
internal data class InstallFailureEntity internal constructor(
	@PrimaryKey
	@ColumnInfo(name = "session_id")
	val sessionId: String,
	@ColumnInfo(name = "failure")
	val failure: InstallFailure
)