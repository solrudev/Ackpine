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

import ru.solrudev.ackpine.uninstaller.parameters.UninstallParameters
import ru.solrudev.ackpine.uninstaller.parameters.UninstallerType

/**
 * A scope for configuring uninstall session parameters controlled by plugins.
 *
 * An instance of this scope is passed to [AckpineUninstallPlugin.apply]. Plugins may mutate it to influence the
 * resulting [UninstallParameters], and may also register additional plugins transitively via [registerPlugin].
 */
public class UninstallPluginScope private constructor(

	/**
	 * Type of the package uninstaller implementation. Default value is [UninstallerType.DEFAULT].
	 */
	public var uninstallerType: UninstallerType
) {

	private val plugins = mutableMapOf<Class<out AckpineUninstallPlugin<*>>, AckpinePlugin.Parameters>()

	/**
	 * Registers a [plugin] for the uninstall session.
	 * @param plugin Java class of a registered plugin, implementing [AckpineUninstallPlugin].
	 * @param parameters parameters of the registered plugin for the session being configured.
	 */
	public fun <Params : AckpinePlugin.Parameters> registerPlugin(
		plugin: Class<out AckpineUninstallPlugin<Params>>,
		parameters: Params
	) {
		plugins[plugin] = parameters
	}

	/**
	 * Registers a [plugin] for the uninstall session.
	 * @param plugin Java class of a registered plugin, implementing [AckpineUninstallPlugin].
	 */
	public fun registerPlugin(plugin: Class<out AckpineUninstallPlugin<AckpinePlugin.Parameters.None>>) {
		plugins[plugin] = AckpinePlugin.Parameters.None
	}

	@JvmSynthetic
	internal fun copy(): UninstallPluginScope {
		val scope = UninstallPluginScope(uninstallerType = uninstallerType)
		scope.plugins.putAll(plugins)
		return scope
	}

	@JvmSynthetic
	internal fun getPlugins() = plugins.toMap()

	@JvmSynthetic
	internal fun resolvePlugins(normalizeUninstallerType: (UninstallerType) -> UninstallerType) {
		val appliedPlugins = mutableSetOf<Class<out AckpineUninstallPlugin<*>>>()
		do {
			uninstallerType = normalizeUninstallerType(uninstallerType)
			val pluginsToApply = getPlugins().keys.filterNot(appliedPlugins::contains)
			for (pluginClass in pluginsToApply) {
				AckpinePluginCache.get(pluginClass).apply(this)
				uninstallerType = normalizeUninstallerType(uninstallerType)
				appliedPlugins += pluginClass
			}
		} while (pluginsToApply.isNotEmpty())
	}

	@JvmSynthetic
	internal fun registerCapabilityPlugin(pluginClass: Class<out AckpineUninstallPlugin<*>>) {
		plugins[pluginClass] = AckpinePlugin.Parameters.None
	}

	internal companion object {
		@JvmSynthetic
		internal fun create(
			uninstallerType: UninstallerType = UninstallerType.DEFAULT
		) = UninstallPluginScope(uninstallerType)
	}
}