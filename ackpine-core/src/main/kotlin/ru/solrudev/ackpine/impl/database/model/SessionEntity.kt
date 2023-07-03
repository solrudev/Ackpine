package ru.solrudev.ackpine.impl.database.model

import androidx.annotation.DrawableRes
import androidx.annotation.RestrictTo
import androidx.room.*
import ru.solrudev.ackpine.installer.parameters.InstallerType
import ru.solrudev.ackpine.session.parameters.Confirmation
import ru.solrudev.ackpine.session.parameters.NotificationString

@RestrictTo(RestrictTo.Scope.LIBRARY)
@Entity(tableName = "sessions")
internal data class SessionEntity internal constructor(
	@PrimaryKey
	@ColumnInfo(name = "id")
	val id: String,
	@ColumnInfo(name = "state", index = true)
	val state: State,
	@ColumnInfo(name = "confirmation")
	val confirmation: Confirmation,
	@ColumnInfo(name = "notification_title")
	val notificationTitle: NotificationString,
	@ColumnInfo(name = "notification_text")
	val notificationText: NotificationString,
	@DrawableRes
	@ColumnInfo(name = "notification_icon")
	val notificationIcon: Int
) {

	@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
	internal enum class State {
		CREATING, PENDING, ACTIVE, AWAITING, COMMITTED, CANCELLED, SUCCEEDED, FAILED
	}

	@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
	internal data class InstallSession internal constructor(
		@Embedded
		val session: SessionEntity,
		@Relation(
			parentColumn = "id",
			entityColumn = "session_id",
			entity = SessionInstallerTypeEntity::class,
			projection = ["installer_type"]
		)
		val installerType: InstallerType,
		@Relation(
			parentColumn = "id",
			entityColumn = "session_id",
			entity = InstallUriEntity::class,
			projection = ["uri"]
		)
		val uris: List<String>
	)

	@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
	internal data class UninstallSession internal constructor(
		@Embedded
		val session: SessionEntity,
		@Relation(
			parentColumn = "id",
			entityColumn = "session_id",
			entity = PackageNameEntity::class,
			projection = ["package_name"]
		)
		val packageName: String
	)
}