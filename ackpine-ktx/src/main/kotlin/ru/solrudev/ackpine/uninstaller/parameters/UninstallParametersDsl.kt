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

@file:Suppress("DEPRECATION")

package ru.solrudev.ackpine.uninstaller.parameters

import ru.solrudev.ackpine.plugability.AckpinePlugin
import ru.solrudev.ackpine.plugability.AckpinePluginRegistryDsl
import ru.solrudev.ackpine.plugability.AckpineUninstallPlugin
import ru.solrudev.ackpine.session.parameters.Confirmation
import ru.solrudev.ackpine.session.parameters.ConfirmationDsl
import ru.solrudev.ackpine.session.parameters.NotificationData
import ru.solrudev.ackpine.session.parameters.SessionParametersDsl
import kotlin.reflect.KClass

/**
 * DSL allowing to configure [parameters for creating uninstall session][UninstallParameters].
 */
@SessionParametersDsl
public interface UninstallParametersDsl : ConfirmationDsl, AckpinePluginRegistryDsl {

	/**
	 * Name of the package to be uninstalled.
	 */
	public var packageName: String

	/**
	 * Type of the package uninstaller implementation.
	 *
	 * Default value is [UninstallerType.DEFAULT].
	 *
	 * When getting/setting the value of this property on API level < 21, [UninstallerType.INTENT_BASED] is always
	 * returned/set regardless of the current/provided value.
	 */
	public var uninstallerType: UninstallerType

	/**
	 * Registers a [plugin] for the uninstall session.
	 * @param plugin Kotlin class of a registered plugin, implementing [AckpineUninstallPlugin].
	 * @param parameters parameters of the registered plugin for the session being configured.
	 */
	public fun <Params : AckpinePlugin.Parameters> plugin(
		plugin: KClass<out AckpineUninstallPlugin<Params>>,
		parameters: Params
	)

	/**
	 * Registers a [plugin] for the uninstall session.
	 * @param plugin Kotlin class of a registered plugin, implementing [AckpineUninstallPlugin].
	 */
	public fun plugin(plugin: KClass<out AckpineUninstallPlugin<AckpinePlugin.Parameters.None>>)
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

	override fun <Params : AckpinePlugin.Parameters> plugin(
		plugin: KClass<out AckpineUninstallPlugin<Params>>,
		parameters: Params
	) {
		builder.registerPlugin(plugin.java, parameters)
	}

	override fun plugin(plugin: KClass<out AckpineUninstallPlugin<AckpinePlugin.Parameters.None>>) {
		builder.registerPlugin(plugin.java)
	}

	@Deprecated(
		"Use typed plugin() methods on InstallParametersDsl or UninstallParametersDsl directly. This will become an error in the next minor version.",
		level = DeprecationLevel.WARNING
	)
	override fun <Params : AckpinePlugin.Parameters> usePlugin(
		plugin: KClass<out AckpinePlugin>,
		parameters: Params
	) {
		builder.usePlugin(plugin.java, parameters)
	}

	@Deprecated(
		"Use typed plugin() methods on InstallParametersDsl or UninstallParametersDsl directly. This will become an error in the next minor version.",
		level = DeprecationLevel.WARNING
	)
	override fun usePlugin(plugin: KClass<out AckpinePlugin>) {
		builder.usePlugin(plugin.java)
	}

	fun build() = builder.build()
}

/**
 * Registers a plugin for the uninstall session. [Plugin] is the type of the plugin being registered.
 */
public inline fun <reified Plugin : AckpineUninstallPlugin<AckpinePlugin.Parameters.None>> UninstallParametersDsl.plugin() {
	plugin(Plugin::class)
}