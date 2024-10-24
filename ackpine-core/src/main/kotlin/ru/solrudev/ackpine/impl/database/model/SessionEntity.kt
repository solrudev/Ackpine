/*
 * Copyright (C) 2023-2024 Ilya Fomichev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.solrudev.ackpine.impl.database.model

import androidx.annotation.DrawableRes
import androidx.annotation.RestrictTo
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import ru.solrudev.ackpine.installer.parameters.InstallerType
import ru.solrudev.ackpine.session.parameters.Confirmation
import ru.solrudev.ackpine.session.parameters.ResolvableString

@RestrictTo(RestrictTo.Scope.LIBRARY)
@Entity(tableName = "sessions")
internal data class SessionEntity internal constructor(
	@PrimaryKey
	@ColumnInfo(name = "id")
	val id: String,
	@ColumnInfo(name = "type", index = true)
	val type: Type,
	@ColumnInfo(name = "state", index = true)
	val state: State,
	@ColumnInfo(name = "confirmation")
	val confirmation: Confirmation,
	@ColumnInfo(name = "notification_title")
	val notificationTitle: ResolvableString,
	@ColumnInfo(name = "notification_text")
	val notificationText: ResolvableString,
	@DrawableRes
	@ColumnInfo(name = "notification_icon")
	val notificationIcon: Int,
	@ColumnInfo(name = "require_user_action", defaultValue = "true")
	val requireUserAction: Boolean,
	@ColumnInfo(name = "last_launch_timestamp", defaultValue = "0", index = true)
	val lastLaunchTimestamp: Long = 0,
	@ColumnInfo(name = "last_commit_timestamp", defaultValue = "0", index = true)
	val lastCommitTimestamp: Long = 0
) {

	@RestrictTo(RestrictTo.Scope.LIBRARY)
	internal enum class Type {
		INSTALL, UNINSTALL
	}

	@RestrictTo(RestrictTo.Scope.LIBRARY)
	internal enum class State {

		PENDING, ACTIVE, AWAITING, COMMITTED, CANCELLED, SUCCEEDED, FAILED;

		internal companion object {

			@JvmSynthetic
			internal const val TERMINAL_STATES = "('CANCELLED', 'SUCCEEDED', 'FAILED')"
		}
	}

	@RestrictTo(RestrictTo.Scope.LIBRARY)
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
		val uris: List<String>,
		@Relation(
			parentColumn = "id",
			entityColumn = "session_id",
			entity = SessionNameEntity::class,
			projection = ["name"]
		)
		val name: String?,
		@Relation(
			parentColumn = "id",
			entityColumn = "session_id",
			entity = NotificationIdEntity::class,
			projection = ["notification_id"]
		)
		val notificationId: Int?,
		@Relation(
			parentColumn = "id",
			entityColumn = "session_id",
			entity = InstallModeEntity::class,
			projection = ["install_mode"]
		)
		val installMode: InstallModeEntity.InstallMode?,
		@Relation(
			parentColumn = "id",
			entityColumn = "session_id",
			entity = PackageNameEntity::class,
			projection = ["package_name"]
		)
		val packageName: String?,
		@Relation(
			parentColumn = "id",
			entityColumn = "session_id",
			entity = LastUpdateTimestampEntity::class,
			projection = ["last_update_timestamp"]
		)
		val lastUpdateTimestamp: Long?
	)

	@RestrictTo(RestrictTo.Scope.LIBRARY)
	internal data class UninstallSession internal constructor(
		@Embedded
		val session: SessionEntity,
		@Relation(
			parentColumn = "id",
			entityColumn = "session_id",
			entity = PackageNameEntity::class,
			projection = ["package_name"]
		)
		val packageName: String,
		@Relation(
			parentColumn = "id",
			entityColumn = "session_id",
			entity = NotificationIdEntity::class,
			projection = ["notification_id"]
		)
		val notificationId: Int?
	)
}