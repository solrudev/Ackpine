package ru.solrudev.ackpine.impl.database

import androidx.annotation.RestrictTo
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ru.solrudev.ackpine.impl.database.converters.InstallFailureConverters
import ru.solrudev.ackpine.impl.database.converters.NotificationStringConverters
import ru.solrudev.ackpine.impl.database.converters.UninstallFailureConverters
import ru.solrudev.ackpine.impl.database.dao.InstallSessionDao
import ru.solrudev.ackpine.impl.database.dao.SessionDao
import ru.solrudev.ackpine.impl.database.dao.SessionProgressDao
import ru.solrudev.ackpine.impl.database.dao.UninstallSessionDao
import ru.solrudev.ackpine.impl.database.model.*

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Database(
	entities = [
		SessionEntity::class,
		SessionInstallerTypeEntity::class,
		InstallFailureEntity::class,
		UninstallFailureEntity::class,
		InstallUriEntity::class,
		PackageNameEntity::class,
		SessionProgressEntity::class
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
}