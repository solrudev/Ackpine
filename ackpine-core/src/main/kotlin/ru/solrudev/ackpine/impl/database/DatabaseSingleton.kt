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

package ru.solrudev.ackpine.impl.database

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import java.util.concurrent.Executor

/**
 * A helper base class for implementing a singleton for a Room database.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public open class DatabaseSingleton<DB : RoomDatabase>(
	private val databaseClass: Class<DB>,
	private val databaseName: String
) {

	private val lock = Any()

	@Volatile
	private var database: DB? = null

	/**
	 * Returns a singleton database instance.
	 * @param context a [Context] for on-demand initialization.
	 * @param executor an [Executor] for asynchronous database operations.
	 */
	@JvmSynthetic
	@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
	public fun getInstance(context: Context, executor: Executor): DB {
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

	protected open fun RoomDatabase.Builder<DB>.configureDatabase(): RoomDatabase.Builder<DB> {
		return this
	}

	private fun create(context: Context, executor: Executor): DB {
		return Room.databaseBuilder(context, databaseClass, databaseName)
			.openHelperFactory { configuration ->
				val config = SupportSQLiteOpenHelper.Configuration.builder(context)
					.name(configuration.name)
					.callback(configuration.callback)
					.noBackupDirectory(true)
					.allowDataLossOnRecovery(true)
					.build()
				FrameworkSQLiteOpenHelperFactory().create(config)
			}
			.setQueryExecutor(executor)
			.configureDatabase()
			.fallbackToDestructiveMigration()
			.build()
	}
}