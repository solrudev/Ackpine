/*
 * Copyright (C) 2024 Ilya Fomichev
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
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import ru.solrudev.ackpine.installer.parameters.InstallConstraints.TimeoutStrategy

@RestrictTo(RestrictTo.Scope.LIBRARY)
@Entity(
	tableName = "sessions_install_constraints",
	foreignKeys = [ForeignKey(
		entity = SessionEntity::class,
		parentColumns = ["id"],
		childColumns = ["session_id"],
		onDelete = ForeignKey.CASCADE,
		onUpdate = ForeignKey.CASCADE
	)]
)
internal class InstallConstraintsEntity(
	@JvmField
	@PrimaryKey
	@ColumnInfo(name = "session_id")
	val sessionId: String,
	@JvmField
	@ColumnInfo(name = "is_app_not_foreground_required")
	val isAppNotForegroundRequired: Boolean,
	@JvmField
	@ColumnInfo(name = "is_app_not_interacting_required")
	val isAppNotInteractingRequired: Boolean,
	@JvmField
	@ColumnInfo(name = "is_app_not_top_visible_required")
	val isAppNotTopVisibleRequired: Boolean,
	@JvmField
	@ColumnInfo(name = "is_device_idle_required")
	val isDeviceIdleRequired: Boolean,
	@JvmField
	@ColumnInfo(name = "is_not_in_call_required")
	val isNotInCallRequired: Boolean,
	@JvmField
	@ColumnInfo(name = "timeout_millis")
	val timeoutMillis: Long,
	@JvmField
	@ColumnInfo(name = "timeout_strategy")
	val timeoutStrategy: TimeoutStrategy,
	@JvmField
	@ColumnInfo(name = "commit_attempts_count")
	val commitAttemptsCount: Int = 0
)