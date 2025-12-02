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

package ru.solrudev.ackpine.uninstaller.parameters

import ru.solrudev.ackpine.session.parameters.Confirmation
import ru.solrudev.ackpine.session.parameters.ConfirmationDsl
import ru.solrudev.ackpine.session.parameters.NotificationData
import ru.solrudev.ackpine.session.parameters.SessionParametersDsl

/**
 * DSL allowing to configure [parameters for creating uninstall session][UninstallParameters].
 */
@SessionParametersDsl
public interface UninstallParametersDsl : ConfirmationDsl {

	/**
	 * Name of the package to be uninstalled.
	 */
	public var packageName: String

	/**
	 * Type of the package uninstaller implementation.
	 *
	 * Default value is [UninstallerType.DEFAULT].
	 */
	public var uninstallerType: UninstallerType
}

@PublishedApi
internal class UninstallParametersDslBuilder(packageName: String) : UninstallParametersDsl {

	private val builder = UninstallParameters.Builder(packageName)

	override var confirmation: Confirmation
		get() = builder.confirmation
		set(value) {
			builder.setConfirmation(value)
		}

	override var notificationData: NotificationData
		get() = builder.notificationData
		set(value) {
			builder.setNotificationData(value)
		}

	override var packageName: String
		get() = builder.packageName
		set(value) {
			builder.setPackageName(value)
		}

	override var uninstallerType: UninstallerType
		get() = builder.uninstallerType
		set(value) {
			builder.setUninstallerType(value)
		}

	fun build() = builder.build()
}