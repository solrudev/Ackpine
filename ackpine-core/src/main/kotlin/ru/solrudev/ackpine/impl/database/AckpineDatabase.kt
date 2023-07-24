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

package ru.solrudev.ackpine.impl.database

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import ru.solrudev.ackpine.impl.database.converters.InstallFailureConverters
import ru.solrudev.ackpine.impl.database.converters.NotificationStringConverters
import ru.solrudev.ackpine.impl.database.converters.UninstallFailureConverters
import ru.solrudev.ackpine.impl.database.dao.InstallSessionDao
import ru.solrudev.ackpine.impl.database.dao.NativeSessionIdDao
import ru.solrudev.ackpine.impl.database.dao.NotificationIdDao
import ru.solrudev.ackpine.impl.database.dao.SessionDao
import ru.solrudev.ackpine.impl.database.dao.SessionProgressDao
import ru.solrudev.ackpine.impl.database.dao.UninstallSessionDao
import ru.solrudev.ackpine.impl.database.model.*
import java.util.concurrent.Executor

private const val ACKPINE_DATABASE_NAME = "ackpine.sessiondb"

@RestrictTo(RestrictTo.Scope.LIBRARY)
@Database(
	entities = [
		SessionEntity::class,
		SessionInstallerTypeEntity::class,
		InstallFailureEntity::class,
		UninstallFailureEntity::class,
		InstallUriEntity::class,
		PackageNameEntity::class,
		SessionProgressEntity::class,
		NativeSessionIdEntity::class,
		NotificationIdEntity::class
	],
	version = 1
)
@TypeConverters(
	value = [InstallFailureConverters::class, UninstallFailureConverters::class, NotificationStringConverters::class]
)
internal abstract class AckpineDatabase : RoomDatabase() {

	abstract fun sessionDao(): SessionDao
	abstract fun sessionProgressDao(): SessionProgressDao
	abstract fun installSessionDao(): InstallSessionDao
	abstract fun uninstallSessionDao(): UninstallSessionDao
	abstract fun nativeSessionIdDao(): NativeSessionIdDao
	abstract fun notificationIdDao(): NotificationIdDao

	internal companion object {

		private val lock = Any()

		@Volatile
		private var database: AckpineDatabase? = null

		@JvmSynthetic
		internal fun getInstance(context: Context, executor: Executor): AckpineDatabase {
			var instance = database
			if (instance != null) {
				return instance
			}
			synchronized(lock) {
				instance = database
				if (instance == null) {
					instance = create(context, executor)
					database = instance
				}
			}
			return instance!!
		}

		private fun create(context: Context, executor: Executor): AckpineDatabase {
			return Room.databaseBuilder(context, AckpineDatabase::class.java, ACKPINE_DATABASE_NAME)
				.openHelperFactory { configuration ->
					val configBuilder = SupportSQLiteOpenHelper.Configuration.builder(context)
					configBuilder.name(configuration.name)
						.callback(configuration.callback)
						.noBackupDirectory(true)
						.allowDataLossOnRecovery(true)
					FrameworkSQLiteOpenHelperFactory().create(configBuilder.build())
				}
				.setQueryExecutor(executor)
				.build()
		}
	}
}