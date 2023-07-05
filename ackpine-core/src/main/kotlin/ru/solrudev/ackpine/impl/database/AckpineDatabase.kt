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
		private var database: AckpineDatabase? = null

		@JvmSynthetic
		internal fun getInstance(context: Context, executor: Executor): AckpineDatabase {
			if (database == null) {
				synchronized(lock) {
					if (database == null) {
						initialize(context, executor)
					}
				}
			}
			return database!!
		}

		private fun initialize(context: Context, executor: Executor) {
			database = Room.databaseBuilder(context, AckpineDatabase::class.java, ACKPINE_DATABASE_NAME)
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