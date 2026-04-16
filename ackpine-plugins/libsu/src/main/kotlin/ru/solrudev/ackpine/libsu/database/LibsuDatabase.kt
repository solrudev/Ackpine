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

package ru.solrudev.ackpine.libsu.database

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

private const val DATABASE_NAME = "ackpine_libsu.paramsdb"

@Database(
	entities = [LibsuInstallParametersEntity::class, LibsuUninstallParametersEntity::class],
	exportSchema = true,
	version = 1
)
internal abstract class LibsuDatabase : RoomDatabase() {

	abstract fun libsuInstallParamsDao(): LibsuInstallParamsDao
	abstract fun libsuUninstallParamsDao(): LibsuUninstallParamsDao

	internal companion object : DatabaseSingleton<LibsuDatabase>(
		databaseClass = LibsuDatabase::class.java,
		databaseName = DATABASE_NAME
	)
}

@Dao
internal interface LibsuInstallParamsDao {

	@Insert(onConflict = OnConflictStrategy.IGNORE)
	fun insertParameters(params: LibsuInstallParametersEntity)

	@Query("SELECT * FROM libsu_install_parameters WHERE session_id = :sessionId")
	fun getBySessionId(sessionId: String): LibsuInstallParametersEntity?
}

@Dao
internal interface LibsuUninstallParamsDao {

	@Insert(onConflict = OnConflictStrategy.IGNORE)
	fun insertParameters(params: LibsuUninstallParametersEntity)

	@Query("SELECT * FROM libsu_uninstall_parameters WHERE session_id = :sessionId")
	fun getBySessionId(sessionId: String): LibsuUninstallParametersEntity?
}

@Entity(tableName = "libsu_install_parameters")
internal class LibsuInstallParametersEntity(
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
	val allUsers: Boolean,
	@JvmField
	@ColumnInfo(name = "installer_package_name", defaultValue = "")
	val installerPackageName: String
)

@Entity(tableName = "libsu_uninstall_parameters")
internal class LibsuUninstallParametersEntity(
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