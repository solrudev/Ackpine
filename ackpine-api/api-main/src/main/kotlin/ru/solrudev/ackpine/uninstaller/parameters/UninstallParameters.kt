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

import ru.solrudev.ackpine.plugability.AckpinePlugin
import ru.solrudev.ackpine.plugability.AckpinePluginCache
import ru.solrudev.ackpine.plugability.AckpinePluginContainer
import ru.solrudev.ackpine.plugability.AckpinePluginRegistry
import ru.solrudev.ackpine.session.parameters.Confirmation
import ru.solrudev.ackpine.session.parameters.ConfirmationAware
import ru.solrudev.ackpine.session.parameters.NotificationData

/**
 * Parameters for creating uninstall session.
 */
public class UninstallParameters private constructor(

	/**
	 * Name of the package to be uninstalled.
	 */
	public val packageName: String,

	/**
	 * Type of the package uninstaller implementation.
	 *
	 * Default value is [UninstallerType.DEFAULT].
	 */
	public val uninstallerType: UninstallerType,

	/**
	 * A strategy for handling user's confirmation of installation or uninstallation.
	 *
	 * Default strategy is [Confirmation.DEFERRED].
	 */
	public override val confirmation: Confirmation,

	/**
	 * Data for a high-priority notification which launches confirmation activity.
	 *
	 * Default value is [NotificationData.DEFAULT].
	 *
	 * Ignored when [confirmation] is [Confirmation.IMMEDIATE].
	 */
	public override val notificationData: NotificationData,

	/**
	 * [Plugins][AckpinePlugin] applied to the uninstall session.
	 */
	public val pluginContainer: AckpinePluginContainer
) : ConfirmationAware {

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false
		other as UninstallParameters
		if (packageName != other.packageName) return false
		if (this@UninstallParameters.uninstallerType != other.uninstallerType) return false
		if (confirmation != other.confirmation) return false
		if (notificationData != other.notificationData) return false
		if (pluginContainer != other.pluginContainer) return false
		return true
	}

	override fun hashCode(): Int {
		var result = packageName.hashCode()
		result = 31 * result + this@UninstallParameters.uninstallerType.hashCode()
		result = 31 * result + confirmation.hashCode()
		result = 31 * result + notificationData.hashCode()
		result = 31 * result + pluginContainer.hashCode()
		return result
	}

	override fun toString(): String {
		return "UninstallParameters(" +
				"packageName='$packageName', " +
				"uninstallerType=${this@UninstallParameters.uninstallerType}, " +
				"confirmation=$confirmation, " +
				"notificationData=$notificationData, " +
				"pluginContainer=$pluginContainer" +
				")"
	}

	/**
	 * Builder for [UninstallParameters].
	 */
	public class Builder(packageName: String) : ConfirmationAware, AckpinePluginRegistry<Builder> {

		private val plugins = mutableMapOf<Class<out AckpinePlugin<*>>, AckpinePlugin.Parameters>()

		/**
		 * Name of the package to be uninstalled.
		 */
		public var packageName: String = packageName
			private set

		/**
		 * Type of the package uninstaller implementation.
		 *
		 * Default value is [UninstallerType.DEFAULT].
		 */
		public var uninstallerType: UninstallerType = UninstallerType.DEFAULT

		/**
		 * A strategy for handling user's confirmation of installation or uninstallation.
		 *
		 * Default strategy is [Confirmation.DEFERRED].
		 */
		public override var confirmation: Confirmation = Confirmation.DEFERRED
			private set

		/**
		 * Data for a high-priority notification which launches confirmation activity.
		 *
		 * Default value is [NotificationData.DEFAULT].
		 *
		 * Ignored when [confirmation] is [Confirmation.IMMEDIATE].
		 */
		public override var notificationData: NotificationData = NotificationData.DEFAULT
			private set

		/**
		 * Sets [UninstallParameters.packageName].
		 */
		public fun setPackageName(packageName: String): Builder = apply {
			this.packageName = packageName
		}

		/**
		 * Sets [UninstallParameters.uninstallerType].
		 */
		public fun setUninstallerType(uninstallerType: UninstallerType): Builder = apply {
			this.uninstallerType = uninstallerType
		}

		/**
		 * Sets [UninstallParameters.confirmation].
		 */
		public fun setConfirmation(confirmation: Confirmation): Builder = apply {
			this.confirmation = confirmation
		}

		/**
		 * Sets [UninstallParameters.notificationData].
		 */
		public fun setNotificationData(notificationData: NotificationData): Builder = apply {
			this.notificationData = notificationData
		}

		override fun <Params : AckpinePlugin.Parameters> usePlugin(
			plugin: Class<out AckpinePlugin<Params>>,
			parameters: Params
		): Builder = apply {
			plugins[plugin] = parameters
		}

		override fun usePlugin(plugin: Class<out AckpinePlugin<AckpinePlugin.Parameters.None>>): Builder = apply {
			plugins[plugin] = AckpinePlugin.Parameters.None
		}

		/**
		 * Constructs a new instance of [UninstallParameters].
		 */
		public fun build(): UninstallParameters {
			val pluginContainer = AckpinePluginContainer.from(plugins)
			val pluginInstances = pluginContainer
				.getPlugins()
				.map { (pluginClass, _) -> AckpinePluginCache.get(pluginClass) }
			for (plugin in pluginInstances) {
				plugin.apply(this)
			}
			return UninstallParameters(packageName, uninstallerType, confirmation, notificationData, pluginContainer)
		}
	}
}