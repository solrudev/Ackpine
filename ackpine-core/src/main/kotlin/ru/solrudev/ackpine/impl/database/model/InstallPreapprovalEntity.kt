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

@RestrictTo(RestrictTo.Scope.LIBRARY)
@Entity(
	tableName = "sessions_install_preapproval",
	foreignKeys = [ForeignKey(
		entity = SessionEntity::class,
		parentColumns = ["id"],
		childColumns = ["session_id"],
		onDelete = ForeignKey.CASCADE,
		onUpdate = ForeignKey.CASCADE
	)]
)
internal data class InstallPreapprovalEntity(
	@PrimaryKey
	@ColumnInfo(name = "session_id")
	val sessionId: String,
	@ColumnInfo(name = "package_name")
	val packageName: String,
	@ColumnInfo(name = "label")
	val label: String,
	@ColumnInfo(name = "locale")
	val locale: String,
	@ColumnInfo(name = "icon")
	val icon: String,
	@ColumnInfo(name = "is_preapproved")
	val isPreapproved: Boolean = false
)