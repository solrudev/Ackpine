/*
 * Copyright (C) 2026 Ilya Fomichev
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

package ru.solrudev.ackpine.dhizuku.database

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import ru.solrudev.ackpine.impl.database.DatabaseSingleton

private const val DATABASE_NAME = "ackpine_dhizuku.paramsdb"

@Database(
	entities = [DhizukuInstallParametersEntity::class, DhizukuUninstallParametersEntity::class],
	exportSchema = true,
	version = 1
)
internal abstract class DhizukuDatabase : RoomDatabase() {

	abstract fun dhizukuInstallParamsDao(): DhizukuInstallParamsDao
	abstract fun dhizukuUninstallParamsDao(): DhizukuUninstallParamsDao

	internal companion object : DatabaseSingleton<DhizukuDatabase>(
		databaseClass = DhizukuDatabase::class.java,
		databaseName = DATABASE_NAME
	)
}

@Dao
internal interface DhizukuInstallParamsDao {

	@Insert(onConflict = OnConflictStrategy.IGNORE)
	fun insertParameters(params: DhizukuInstallParametersEntity)

	@Query("SELECT * FROM dhizuku_install_parameters WHERE session_id = :sessionId")
	fun getBySessionId(sessionId: String): DhizukuInstallParametersEntity?
}

@Dao
internal interface DhizukuUninstallParamsDao {

	@Insert(onConflict = OnConflictStrategy.IGNORE)
	fun insertParameters(params: DhizukuUninstallParametersEntity)

	@Query("SELECT * FROM dhizuku_uninstall_parameters WHERE session_id = :sessionId")
	fun getBySessionId(sessionId: String): DhizukuUninstallParametersEntity?
}

@Entity(tableName = "dhizuku_install_parameters")
internal class DhizukuInstallParametersEntity(
	@JvmField
	@PrimaryKey
	@ColumnInfo(name = "session_id")
	val sessionId: String,
	@JvmField
	@ColumnInfo(name = "request_downgrade")
	val requestDowngrade: Boolean
)

@Entity(tableName = "dhizuku_uninstall_parameters")
internal class DhizukuUninstallParametersEntity(
	@JvmField
	@PrimaryKey
	@ColumnInfo(name = "session_id")
	val sessionId: String,
	@JvmField
	@ColumnInfo(name = "keep_data")
	val keepData: Boolean,
	@JvmField
	@ColumnInfo(name = "all_users")
	val allUsers: Boolean,
	@JvmField
	@ColumnInfo(name = "system_app")
	val systemApp: Boolean
)