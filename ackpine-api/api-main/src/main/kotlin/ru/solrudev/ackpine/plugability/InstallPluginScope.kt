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

package ru.solrudev.ackpine.plugability

import ru.solrudev.ackpine.installer.parameters.InstallConstraints
import ru.solrudev.ackpine.installer.parameters.InstallParameters
import ru.solrudev.ackpine.installer.parameters.InstallPreapproval
import ru.solrudev.ackpine.installer.parameters.InstallerType

/**
 * A scope for configuring install session parameters controlled by plugins.
 *
 * An instance of this scope is passed to [AckpineInstallPlugin.apply]. Plugins may mutate it to influence the
 * resulting [InstallParameters], and may also register additional plugins transitively via [registerPlugin].
 */
public class InstallPluginScope private constructor(

	/**
	 * Type of the package installer implementation.
	 *
	 * Default value is [InstallerType.DEFAULT].
	 */
	public var installerType: InstallerType,

	requireUserAction: Boolean = true,
	requestUpdateOwnership: Boolean = false,
	preapproval: InstallPreapproval = InstallPreapproval.NONE,
	constraints: InstallConstraints = InstallConstraints.NONE
) {

	/**
	 * Indicates whether user action should be required when the session is committed. Default value is `true`.
	 */
	public var requireUserAction: Boolean = requireUserAction
		set(value) {
			field = value
			isUserActionForced = value
		}

	/**
	 * Indicates whether the package being installed needs the update ownership enforcement. Default value is `false`.
	 */
	public var requestUpdateOwnership: Boolean = requestUpdateOwnership
		set(value) {
			field = value
			isUpdateOwnershipDisabled = !value
		}


	/**
	 * Details for requesting the pre-commit install approval. Default value is [InstallPreapproval.NONE].
	 *
	 * Use [disablePreapproval] to reset this to [InstallPreapproval.NONE].
	 */
	@set:JvmSynthetic
	public var preapproval: InstallPreapproval = preapproval
		internal set

	/**
	 * Installation constraints. Default value is [InstallConstraints.NONE].
	 *
	 * Use [disableConstraints] to reset this to [InstallConstraints.NONE].
	 */
	@set:JvmSynthetic
	public var constraints: InstallConstraints = constraints
		internal set

	private val plugins = mutableMapOf<Class<out AckpineInstallPlugin<*>>, AckpinePlugin.Parameters>()

	@get:JvmSynthetic
	internal var isPreapprovalDisabled = false
		private set

	@get:JvmSynthetic
	internal var isConstraintsDisabled = false
		private set

	@get:JvmSynthetic
	internal var isUserActionForced = false
		private set

	@get:JvmSynthetic
	internal var isUpdateOwnershipDisabled = false
		private set

	/**
	 * Resets [preapproval] to [InstallPreapproval.NONE].
	 */
	public fun disablePreapproval() {
		preapproval = InstallPreapproval.NONE
		isPreapprovalDisabled = true
	}

	/**
	 * Resets [constraints] to [InstallConstraints.NONE].
	 */
	public fun disableConstraints() {
		constraints = InstallConstraints.NONE
		isConstraintsDisabled = true
	}

	/**
	 * Registers a [plugin] for the install session.
	 * @param plugin Java class of a registered plugin, implementing [AckpineInstallPlugin].
	 * @param parameters parameters of the registered plugin for the session being configured.
	 */
	public fun <Params : AckpinePlugin.Parameters> registerPlugin(
		plugin: Class<out AckpineInstallPlugin<Params>>,
		parameters: Params
	) {
		plugins[plugin] = parameters
	}

	/**
	 * Registers a [plugin] for the install session.
	 * @param plugin Java class of a registered plugin, implementing [AckpineInstallPlugin].
	 */
	public fun registerPlugin(plugin: Class<out AckpineInstallPlugin<AckpinePlugin.Parameters.None>>) {
		plugins[plugin] = AckpinePlugin.Parameters.None
	}

	@JvmSynthetic
	internal fun copy(): InstallPluginScope {
		val scope = InstallPluginScope(
			installerType = installerType,
			requireUserAction = requireUserAction,
			requestUpdateOwnership = requestUpdateOwnership,
			preapproval = preapproval,
			constraints = constraints
		)
		scope.plugins.putAll(plugins)
		return scope
	}

	@JvmSynthetic
	internal fun getPlugins() = plugins.toMap()

	@JvmSynthetic
	internal fun resolvePlugins(normalizeInstallerType: (InstallerType) -> InstallerType) {
		val appliedPlugins = mutableSetOf<Class<out AckpineInstallPlugin<*>>>()
		do {
			installerType = normalizeInstallerType(installerType)
			val pluginsToApply = getPlugins().keys.filterNot(appliedPlugins::contains)
			for (pluginClass in pluginsToApply) {
				AckpinePluginCache.get(pluginClass).apply(this)
				installerType = normalizeInstallerType(installerType)
				appliedPlugins += pluginClass
			}
		} while (pluginsToApply.isNotEmpty())
	}

	@JvmSynthetic
	internal fun registerCapabilityPlugin(pluginClass: Class<out AckpineInstallPlugin<*>>) {
		plugins[pluginClass] = AckpinePlugin.Parameters.None
	}

	internal companion object {
		@JvmSynthetic
		internal fun create(installerType: InstallerType = InstallerType.DEFAULT) = InstallPluginScope(installerType)
	}
}