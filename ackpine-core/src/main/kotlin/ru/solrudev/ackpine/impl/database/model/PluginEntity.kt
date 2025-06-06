/*
 * Copyright (C) 2025 Ilya Fomichev
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
import ru.solrudev.ackpine.plugability.AckpinePlugin

@RestrictTo(RestrictTo.Scope.LIBRARY)
@Entity(
	tableName = "sessions_plugins",
	foreignKeys = [ForeignKey(
		entity = SessionEntity::class,
		parentColumns = ["id"],
		childColumns = ["session_id"],
		onDelete = ForeignKey.CASCADE,
		onUpdate = ForeignKey.CASCADE
	)]
)
internal class PluginEntity internal constructor(
	@JvmField
	@PrimaryKey(autoGenerate = true)
	@ColumnInfo(name = "id")
	val id: Int = 0,
	@JvmField
	@ColumnInfo(name = "session_id", index = true)
	val sessionId: String,
	@JvmField
	@ColumnInfo(name = "plugin_class_name")
	val pluginClassName: String,
	@JvmField
	@ColumnInfo(name = "plugin_parameters")
	val pluginParameters: AckpinePlugin.Parameters
)