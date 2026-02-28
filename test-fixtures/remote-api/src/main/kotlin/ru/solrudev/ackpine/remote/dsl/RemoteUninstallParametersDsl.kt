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

package ru.solrudev.ackpine.remote.dsl

import ru.solrudev.ackpine.remote.RemoteNotificationData
import ru.solrudev.ackpine.session.parameters.Confirmation
import ru.solrudev.ackpine.session.parameters.SessionParametersDsl
import ru.solrudev.ackpine.uninstaller.parameters.UninstallParameters
import ru.solrudev.ackpine.uninstaller.parameters.UninstallerType

/**
 * DSL allowing to configure parameters for creating uninstall session.
 */
@SessionParametersDsl
public interface RemoteUninstallParametersDsl : RemoteConfirmationDsl {

	/**
	 * Type of the package uninstaller implementation.
	 *
	 * Default value is [UninstallerType.DEFAULT].
	 */
	public var uninstallerType: UninstallerType
}

internal class RemoteUninstallParameters(
	val uninstallParameters: UninstallParameters,
	val notificationData: RemoteNotificationData
) {
	internal class Builder(val parametersBuilder: UninstallParameters.Builder) {
		fun build(notificationData: RemoteNotificationData) = RemoteUninstallParameters(
			parametersBuilder.build(),
			notificationData
		)
	}
}

@PublishedApi
internal class RemoteUninstallParametersDslBuilder(packageName: String) : RemoteUninstallParametersDsl {

	private val builder = RemoteUninstallParameters.Builder(UninstallParameters.Builder(packageName))

	override var uninstallerType: UninstallerType
		get() = builder.parametersBuilder.uninstallerType
		set(value) {
			builder.parametersBuilder.setUninstallerType(value)
		}

	override var confirmation: Confirmation
		get() = builder.parametersBuilder.confirmation
		set(value) {
			builder.parametersBuilder.setConfirmation(value)
		}

	override var notificationData = RemoteNotificationData.DEFAULT

	fun build() = builder.build(notificationData)
}