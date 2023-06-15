package ru.solrudev.ackpine.impl.database.converters

import androidx.room.TypeConverter
import ru.solrudev.ackpine.installer.InstallFailure
import ru.solrudev.ackpine.session.parameters.NotificationString
import ru.solrudev.ackpine.uninstaller.UninstallFailure

internal object NotificationStringConverters {

	@TypeConverter
	@JvmStatic
	internal fun fromByteArray(byteArray: ByteArray): NotificationString = byteArray.deserialize()

	@TypeConverter
	@JvmStatic
	internal fun toByteArray(notificationString: NotificationString): ByteArray = notificationString.serialize()
}

internal object InstallFailureConverters {

	@TypeConverter
	@JvmStatic
	internal fun fromByteArray(byteArray: ByteArray): InstallFailure = byteArray.deserialize()

	@TypeConverter
	@JvmStatic
	internal fun toByteArray(installFailure: InstallFailure): ByteArray = installFailure.serialize()
}

internal object UninstallFailureConverters {

	@TypeConverter
	@JvmStatic
	internal fun fromByteArray(byteArray: ByteArray): UninstallFailure = byteArray.deserialize()

	@TypeConverter
	@JvmStatic
	internal fun toByteArray(uninstallFailure: UninstallFailure): ByteArray = uninstallFailure.serialize()
}