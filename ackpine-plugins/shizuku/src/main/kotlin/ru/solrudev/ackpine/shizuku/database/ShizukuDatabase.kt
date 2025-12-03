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

package ru.solrudev.ackpine.shizuku.database

import androidx.room.AutoMigration
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import ru.solrudev.ackpine.impl.database.DatabaseSingleton

private const val DATABASE_NAME = "ackpine_shizuku.paramsdb"

@Database(
	entities = [ShizukuParametersEntity::class, ShizukuUninstallParametersEntity::class],
	exportSchema = true,
	autoMigrations = [AutoMigration(from = 1, to = 2)],
	version = 2
)
internal abstract class ShizukuDatabase : RoomDatabase() {

	abstract fun shizukuParamsDao(): ShizukuParamsDao
	abstract fun shizukuUninstallParamsDao(): ShizukuUninstallParamsDao

	internal companion object : DatabaseSingleton<ShizukuDatabase>(
		databaseClass = ShizukuDatabase::class.java,
		databaseName = DATABASE_NAME
	)
}

@Dao
internal interface ShizukuParamsDao {

	@Insert
	fun insertParameters(params: ShizukuParametersEntity)

	@Query("SELECT * FROM shizuku_parameters WHERE session_id = :sessionId")
	fun getBySessionId(sessionId: String): ShizukuParametersEntity
}

@Dao
internal interface ShizukuUninstallParamsDao {

	@Insert
	fun insertParameters(params: ShizukuUninstallParametersEntity)

	@Query("SELECT * FROM shizuku_uninstall_parameters WHERE session_id = :sessionId")
	fun getBySessionId(sessionId: String): ShizukuUninstallParametersEntity
}

@Entity(tableName = "shizuku_parameters")
internal class ShizukuParametersEntity(
	@JvmField
	@PrimaryKey
	@ColumnInfo(name = "session_id")
	val sessionId: String,
	@JvmField
	@ColumnInfo(name = "bypass_low_target_sdk_block")
	val bypassLowTargetSdkBlock: Boolean,
	@JvmField
	@ColumnInfo(name = "allow_test")
	val allowTest: Boolean,
	@JvmField
	@ColumnInfo(name = "replace_existing")
	val replaceExisting: Boolean,
	@JvmField
	@ColumnInfo(name = "request_downgrade")
	val requestDowngrade: Boolean,
	@JvmField
	@ColumnInfo(name = "grant_all_requested_permissions")
	val grantAllRequestedPermissions: Boolean,
	@JvmField
	@ColumnInfo(name = "all_users")
	val allUsers: Boolean
)

@Entity(tableName = "shizuku_uninstall_parameters")
internal class ShizukuUninstallParametersEntity(
	@JvmField
	@PrimaryKey
	@ColumnInfo(name = "session_id")
	val sessionId: String,
	@JvmField
	@ColumnInfo(name = "keep_data")
	val keepData: Boolean,
	@JvmField
	@ColumnInfo(name = "all_users")
	val allUsers: Boolean
)