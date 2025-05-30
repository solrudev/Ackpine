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

package ru.solrudev.ackpine.impl.database.converters

import androidx.room.TypeConverter
import ru.solrudev.ackpine.installer.InstallFailure
import ru.solrudev.ackpine.installer.parameters.InstallConstraints.TimeoutStrategy
import ru.solrudev.ackpine.installer.parameters.PackageSource
import ru.solrudev.ackpine.installer.parameters.PackageSource.Unspecified
import ru.solrudev.ackpine.installer.parameters.packageSources
import ru.solrudev.ackpine.plugability.AckpinePlugin
import ru.solrudev.ackpine.resources.ResolvableString
import ru.solrudev.ackpine.session.parameters.DrawableId
import ru.solrudev.ackpine.uninstaller.UninstallFailure

internal object DrawableIdConverters {

	@TypeConverter
	@JvmStatic
	internal fun fromByteArray(byteArray: ByteArray): DrawableId = byteArray.deserialize()

	@TypeConverter
	@JvmStatic
	internal fun toByteArray(drawableId: DrawableId): ByteArray = drawableId.serialize()
}

internal object ResolvableStringConverters {

	@TypeConverter
	internal fun fromByteArray(byteArray: ByteArray): ResolvableString = byteArray.deserialize()

	@TypeConverter
	internal fun toByteArray(resolvableString: ResolvableString): ByteArray = resolvableString.serialize()
}

internal object InstallFailureConverters {

	@TypeConverter
	internal fun fromByteArray(byteArray: ByteArray): InstallFailure = byteArray.deserialize()

	@TypeConverter
	internal fun toByteArray(installFailure: InstallFailure): ByteArray = installFailure.serialize()
}

internal object UninstallFailureConverters {

	@TypeConverter
	internal fun fromByteArray(byteArray: ByteArray): UninstallFailure = byteArray.deserialize()

	@TypeConverter
	internal fun toByteArray(uninstallFailure: UninstallFailure): ByteArray = uninstallFailure.serialize()
}

internal object TimeoutStrategyConverters {

	@TypeConverter
	internal fun fromByteArray(byteArray: ByteArray): TimeoutStrategy = byteArray.deserialize()

	@TypeConverter
	internal fun toByteArray(timeoutStrategy: TimeoutStrategy): ByteArray = timeoutStrategy.serialize()
}

internal object PackageSourceConverters {

	@TypeConverter
	internal fun fromOrdinal(ordinal: Int) = packageSources.getOrNull(ordinal) ?: Unspecified

	@TypeConverter
	internal fun toByteArray(packageSource: PackageSource) = packageSource.ordinal
}

internal object AckpinePluginParametersConverters {

	@TypeConverter
	internal fun fromByteArray(byteArray: ByteArray): AckpinePlugin.Parameters<AckpinePlugin> = byteArray.deserialize()

	@TypeConverter
	internal fun toByteArray(params: AckpinePlugin.Parameters<AckpinePlugin>): ByteArray = params.serialize()
}