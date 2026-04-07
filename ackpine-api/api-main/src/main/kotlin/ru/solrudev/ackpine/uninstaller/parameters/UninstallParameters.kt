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

import ru.solrudev.ackpine.isPackageInstallerApiAvailable
import ru.solrudev.ackpine.plugability.AckpinePlugin
import ru.solrudev.ackpine.plugability.AckpinePluginCache
import ru.solrudev.ackpine.plugability.AckpinePluginContainer
import ru.solrudev.ackpine.plugability.AckpinePluginRegistry
import ru.solrudev.ackpine.plugability.AckpineUninstallPlugin
import ru.solrudev.ackpine.plugability.UninstallPluginScope
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
		if (uninstallerType != other.uninstallerType) return false
		if (confirmation != other.confirmation) return false
		if (notificationData != other.notificationData) return false
		if (pluginContainer != other.pluginContainer) return false
		return true
	}

	override fun hashCode(): Int {
		var result = packageName.hashCode()
		result = 31 * result + uninstallerType.hashCode()
		result = 31 * result + confirmation.hashCode()
		result = 31 * result + notificationData.hashCode()
		result = 31 * result + pluginContainer.hashCode()
		return result
	}

	override fun toString(): String {
		return "UninstallParameters(" +
				"packageName='$packageName', " +
				"uninstallerType=$uninstallerType, " +
				"confirmation=$confirmation, " +
				"notificationData=$notificationData, " +
				"pluginContainer=$pluginContainer" +
				")"
	}

	/**
	 * Builder for [UninstallParameters].
	 */
	public class Builder : ConfirmationAware, AckpinePluginRegistry<Builder> {

		public constructor(packageName: String) {
			this.packageName = packageName
			pluginScope = UninstallPluginScope.create()
		}

		private constructor(packageName: String, scope: UninstallPluginScope) {
			this.packageName = packageName
			pluginScope = scope
		}

		@get:JvmSynthetic
		internal val pluginScope: UninstallPluginScope

		/**
		 * Name of the package to be uninstalled.
		 */
		public var packageName: String
			private set

		/**
		 * Type of the package uninstaller implementation.
		 *
		 * Default value is [UninstallerType.DEFAULT].
		 *
		 * When getting/setting the value of this property on API level < 21, [UninstallerType.INTENT_BASED] is always
		 * returned/set regardless of the current/provided value.
		 */
		public var uninstallerType: UninstallerType
			get() = pluginScope.normalizeUninstallerType()
			private set(value) {
				pluginScope.normalizeUninstallerType(value)
			}

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
			@Suppress("DEPRECATION_ERROR")
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

		/**
		 * Registers a [plugin] for the uninstall session.
		 * @param plugin Java class of a registered plugin, implementing [AckpineUninstallPlugin].
		 * @param parameters parameters of the registered plugin for the session being configured.
		 */
		public fun <Params : AckpinePlugin.Parameters> registerPlugin(
			plugin: Class<out AckpineUninstallPlugin<Params>>,
			parameters: Params
		): Builder = apply {
			pluginScope.registerPlugin(plugin, parameters)
		}

		/**
		 * Registers a [plugin] for the uninstall session.
		 * @param plugin Java class of a registered plugin, implementing [AckpineUninstallPlugin].
		 */
		public fun registerPlugin(
			plugin: Class<out AckpineUninstallPlugin<AckpinePlugin.Parameters.None>>
		): Builder = apply {
			pluginScope.registerPlugin(plugin)
		}

		@Deprecated(
			"Use typed registerPlugin methods. This will become an error in the next minor version. " +
					"Untyped plugins (implementing AckpinePlugin directly) will throw when used.",
			level = DeprecationLevel.WARNING
		)
		@Suppress("UNCHECKED_CAST")
		override fun <Params : AckpinePlugin.Parameters> usePlugin(
			plugin: Class<out AckpinePlugin>,
			parameters: Params
		): Builder = apply {
			if (!AckpineUninstallPlugin::class.java.isAssignableFrom(plugin)) {
				error("Not an uninstall plugin: ${plugin.name}")
			}
			pluginScope.registerPlugin(plugin as Class<AckpineUninstallPlugin<Params>>, parameters)
		}

		@Deprecated(
			"Use typed registerPlugin methods. This will become an error in the next minor version. " +
					"Untyped plugins (implementing AckpinePlugin directly) will throw when used.",
			level = DeprecationLevel.WARNING
		)
		@Suppress("UNCHECKED_CAST")
		override fun usePlugin(plugin: Class<out AckpinePlugin>): Builder = apply {
			if (!AckpineUninstallPlugin::class.java.isAssignableFrom(plugin)) {
				error("Not an uninstall plugin: ${plugin.name}")
			}
			pluginScope.registerPlugin(plugin as Class<AckpineUninstallPlugin<AckpinePlugin.Parameters.None>>)
		}

		/**
		 * Constructs a new instance of [UninstallParameters].
		 */
		public fun build(): UninstallParameters {
			val snapshot = createUninstallSnapshot()
			snapshot.applyPlugins()
			return UninstallParameters(
				snapshot.packageName,
				snapshot.uninstallerType,
				snapshot.confirmation,
				snapshot.notificationData,
				AckpinePluginContainer.from(snapshot.pluginScope.getPlugins())
			)
		}

		private fun applyPlugins() {
			val appliedPlugins = mutableSetOf<Class<out AckpineUninstallPlugin<*>>>()
			var pluginsToApply: List<Class<out AckpineUninstallPlugin<*>>>
			do {
				pluginScope.normalizeUninstallerType()
				pluginsToApply = pluginScope
					.getPlugins()
					.keys
					.filterNot(appliedPlugins::contains)
				for (pluginClass in pluginsToApply) {
					AckpinePluginCache.get(pluginClass).apply(this)
					pluginScope.normalizeUninstallerType()
					appliedPlugins += pluginClass
				}
			} while (pluginsToApply.isNotEmpty())
		}

		private fun createUninstallSnapshot() = Builder(packageName, pluginScope.copy())
			.setConfirmation(confirmation)
			.setNotificationData(notificationData)

		private fun UninstallPluginScope.normalizeUninstallerType(
			value: UninstallerType = this.uninstallerType
		): UninstallerType {
			val uninstallerType = applyUninstallerTypeInvariants(value)
			this.uninstallerType = uninstallerType
			return uninstallerType
		}

		private fun applyUninstallerTypeInvariants(value: UninstallerType) = when {
			isPackageInstallerApiAvailable() -> value
			else -> UninstallerType.INTENT_BASED
		}
	}
}