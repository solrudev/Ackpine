package ru.solrudev.ackpine.impl.database.model

import androidx.annotation.RestrictTo
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import ru.solrudev.ackpine.installer.parameters.InstallerType

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Entity(
	tableName = "sessions_installer_types",
	foreignKeys = [ForeignKey(
		entity = SessionEntity::class,
		parentColumns = ["id"],
		childColumns = ["session_id"],
		onDelete = ForeignKey.CASCADE,
		onUpdate = ForeignKey.CASCADE
	)]
)
internal data class SessionInstallerTypeEntity internal constructor(
	@PrimaryKey
	@ColumnInfo(name = "session_id")
	val sessionId: String,
	@ColumnInfo(name = "installer_type")
	val installerType: InstallerType
)