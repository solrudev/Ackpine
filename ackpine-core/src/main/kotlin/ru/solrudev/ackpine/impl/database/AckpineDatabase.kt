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
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import ru.solrudev.ackpine.impl.database.converters.InstallFailureConverters
import ru.solrudev.ackpine.impl.database.converters.NotificationStringConverters
import ru.solrudev.ackpine.impl.database.converters.UninstallFailureConverters
import ru.solrudev.ackpine.impl.database.dao.InstallSessionDao
import ru.solrudev.ackpine.impl.database.dao.NativeSessionIdDao
import ru.solrudev.ackpine.impl.database.dao.NotificationIdDao
import ru.solrudev.ackpine.impl.database.dao.SessionDao
import ru.solrudev.ackpine.impl.database.dao.SessionNameDao
import ru.solrudev.ackpine.impl.database.dao.SessionProgressDao
import ru.solrudev.ackpine.impl.database.dao.UninstallSessionDao
import ru.solrudev.ackpine.impl.database.model.*
import ru.solrudev.ackpine.impl.database.model.SessionEntity.State.Companion.TERMINAL_STATES
import java.util.concurrent.Executor
import kotlin.time.Duration.Companion.days

private const val ACKPINE_DATABASE_NAME = "ackpine.sessiondb"
private const val PURGE_SQL = "DELETE FROM sessions WHERE state IN $TERMINAL_STATES AND last_launch_timestamp <"

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
		NotificationIdEntity::class,
		SessionNameEntity::class
	],
	autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3)],
	version = 3,
	exportSchema = true
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
	abstract fun sessionNameDao(): SessionNameDao

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
				.addCallback(PurgeCallback)
				.fallbackToDestructiveMigration()
				.build()
		}
	}
}

private object PurgeCallback : RoomDatabase.Callback() {

	private val eligibleForPurgeTimestamp: Long
		get() = System.currentTimeMillis() - 1.days.inWholeMilliseconds

	override fun onOpen(db: SupportSQLiteDatabase) {
		db.beginTransaction()
		try {
			db.execSQL("$PURGE_SQL $eligibleForPurgeTimestamp")
			db.setTransactionSuccessful()
		} finally {
			db.endTransaction()
		}
	}
}