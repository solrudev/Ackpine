/*
 * Copyright (C) 2023 Ilya Fomichev
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

import androidx.annotation.RestrictTo
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import ru.solrudev.ackpine.installer.parameters.InstallerType
import ru.solrudev.ackpine.installer.parameters.PackageSource
import ru.solrudev.ackpine.resources.ResolvableString
import ru.solrudev.ackpine.session.parameters.Confirmation
import ru.solrudev.ackpine.session.parameters.DrawableId
import ru.solrudev.ackpine.uninstaller.parameters.UninstallerType

@RestrictTo(RestrictTo.Scope.LIBRARY)
@Entity(tableName = "sessions")
internal class SessionEntity internal constructor(
	@JvmField
	@PrimaryKey
	@ColumnInfo(name = "id")
	val id: String,
	@JvmField
	@ColumnInfo(name = "type", index = true)
	val type: Type,
	@JvmField
	@ColumnInfo(name = "state", index = true)
	val state: State,
	@JvmField
	@ColumnInfo(name = "confirmation")
	val confirmation: Confirmation,
	@JvmField
	@ColumnInfo(name = "notification_title")
	val notificationTitle: ResolvableString,
	@JvmField
	@ColumnInfo(name = "notification_text")
	val notificationText: ResolvableString,
	@JvmField
	@ColumnInfo(name = "notification_icon")
	val notificationIcon: DrawableId,
	@JvmField
	@ColumnInfo(name = "require_user_action", defaultValue = "true")
	val requireUserAction: Boolean,
	@JvmField
	@ColumnInfo(name = "last_launch_timestamp", defaultValue = "0", index = true)
	val lastLaunchTimestamp: Long = 0,
	@JvmField
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

		val isTerminal get() = this == CANCELLED || this == SUCCEEDED || this == FAILED

		internal companion object {

			@JvmSynthetic
			internal const val TERMINAL_STATES = "('CANCELLED', 'SUCCEEDED', 'FAILED')"
		}
	}

	@RestrictTo(RestrictTo.Scope.LIBRARY)
	internal class InstallSession internal constructor(
		@Embedded
		override val session: SessionEntity,
		@JvmField
		@Relation(
			parentColumn = "id",
			entityColumn = "session_id",
			entity = SessionInstallerTypeEntity::class,
			projection = ["installer_type"]
		)
		val installerType: InstallerType,
		@JvmField
		@Relation(
			parentColumn = "id",
			entityColumn = "session_id",
			entity = InstallUriEntity::class,
			projection = ["uri"]
		)
		val uris: List<String>,
		@Relation(
			parentColumn = "id",
			entityColumn = "session_id"
		)
		override val plugins: List<PluginEntity>,
		@JvmField
		@Relation(
			parentColumn = "id",
			entityColumn = "session_id",
			entity = SessionNameEntity::class,
			projection = ["name"]
		)
		val name: String?,
		@JvmField
		@Relation(
			parentColumn = "id",
			entityColumn = "session_id",
			entity = NotificationIdEntity::class,
			projection = ["notification_id"]
		)
		val notificationId: Int?,
		@JvmField
		@Relation(
			parentColumn = "id",
			entityColumn = "session_id"
		)
		val installMode: InstallModeEntity?,
		@JvmField
		@Relation(
			parentColumn = "id",
			entityColumn = "session_id",
			entity = PackageNameEntity::class,
			projection = ["package_name"]
		)
		val packageName: String?,
		@JvmField
		@Relation(
			parentColumn = "id",
			entityColumn = "session_id",
			entity = LastUpdateTimestampEntity::class,
			projection = ["last_update_timestamp"]
		)
		val lastUpdateTimestamp: Long?,
		@JvmField
		@Relation(
			parentColumn = "id",
			entityColumn = "session_id"
		)
		val preapproval: InstallPreapprovalEntity?,
		@JvmField
		@Relation(
			parentColumn = "id",
			entityColumn = "session_id"
		)
		val constraints: InstallConstraintsEntity?,
		@JvmField
		@Relation(
			parentColumn = "id",
			entityColumn = "session_id",
			entity = UpdateOwnershipEntity::class,
			projection = ["request_update_ownership"]
		)
		val requestUpdateOwnership: Boolean?,
		@JvmField
		@Relation(
			parentColumn = "id",
			entityColumn = "session_id",
			entity = PackageSourceEntity::class,
			projection = ["package_source"]
		)
		val packageSource: PackageSource?,
		@JvmField
		@Relation(
			parentColumn = "id",
			entityColumn = "session_id",
			entity = NativeSessionIdEntity::class,
			projection = ["native_session_id"]
		)
		val nativeSessionId: Int? = -1,
		@JvmField
		@Relation(
			parentColumn = "id",
			entityColumn = "session_id",
			entity = ConfirmationLaunchEntity::class,
			projection = ["was_confirmation_launched"]
		)
		val wasConfirmationLaunched: Boolean? = false
	) : HasSession, HasPlugins

	@RestrictTo(RestrictTo.Scope.LIBRARY)
	internal class UninstallSession internal constructor(
		@Embedded
		override val session: SessionEntity,
		@JvmField
		@Relation(
			parentColumn = "id",
			entityColumn = "session_id",
			entity = PackageNameEntity::class,
			projection = ["package_name"]
		)
		val packageName: String,
		@JvmField
		@Relation(
			parentColumn = "id",
			entityColumn = "session_id",
			entity = SessionUninstallerTypeEntity::class,
			projection = ["uninstaller_type"]
		)
		val uninstallerType: UninstallerType,
		@JvmField
		@Relation(
			parentColumn = "id",
			entityColumn = "session_id",
			entity = NotificationIdEntity::class,
			projection = ["notification_id"]
		)
		val notificationId: Int?,
		@Relation(
			parentColumn = "id",
			entityColumn = "session_id"
		)
		override val plugins: List<PluginEntity>
	) : HasSession, HasPlugins
}

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal interface HasSession {
	val session: SessionEntity
}

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal interface HasPlugins {
	val plugins: List<PluginEntity>
}