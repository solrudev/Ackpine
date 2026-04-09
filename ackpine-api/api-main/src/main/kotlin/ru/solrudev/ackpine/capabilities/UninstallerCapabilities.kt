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

package ru.solrudev.ackpine.capabilities

import ru.solrudev.ackpine.plugability.AckpineUninstallPlugin
import ru.solrudev.ackpine.uninstaller.PackageUninstaller
import ru.solrudev.ackpine.uninstaller.parameters.UninstallerType

/**
 * Capabilities of a [PackageUninstaller] configuration as determined by [PackageUninstaller.getCapabilities].
 *
 * The [uninstallerType] field reflects the effective backend after normalization and plugin resolution, which may
 * differ from the requested value (e.g., when the requested backend is not supported by the current Android version).
 */
public class UninstallerCapabilities internal constructor(

	/**
	 * The effective uninstaller type after normalization and plugin resolution.
	 */
	public val uninstallerType: UninstallerType,

	private val pluginCapabilities: Map<Class<out UninstallCapabilityProvider<*>>, PluginCapability>
) {

	/**
	 * Returns the plugin-specific uninstall capabilities for the given [pluginClass], or `null` if the plugin was not
	 * in the resolved graph.
	 * @param pluginClass Java class of the plugin, implementing both [AckpineUninstallPlugin] and
	 * [UninstallCapabilityProvider].
	 */
	public fun <Plugin, C> plugin(pluginClass: Class<out Plugin>): C?
			where Plugin : AckpineUninstallPlugin<*>,
				  Plugin : UninstallCapabilityProvider<C>,
				  C : PluginCapability {
		@Suppress("UNCHECKED_CAST")
		return pluginCapabilities[pluginClass] as C?
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is UninstallerCapabilities) return false
		if (uninstallerType != other.uninstallerType) return false
		if (pluginCapabilities != other.pluginCapabilities) return false
		return true
	}

	override fun hashCode(): Int {
		var result = uninstallerType.hashCode()
		result = 31 * result + pluginCapabilities.hashCode()
		return result
	}

	override fun toString(): String {
		return "UninstallerCapabilities(" +
				"uninstallerType=$uninstallerType, " +
				"pluginCapabilities=$pluginCapabilities" +
				")"
	}
}