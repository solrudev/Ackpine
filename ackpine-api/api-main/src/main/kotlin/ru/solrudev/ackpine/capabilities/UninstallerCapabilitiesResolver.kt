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

import ru.solrudev.ackpine.SdkIntWrapper
import ru.solrudev.ackpine.isPackageInstallerApiAvailable
import ru.solrudev.ackpine.plugability.AckpinePluginCache
import ru.solrudev.ackpine.plugability.AckpineUninstallPlugin
import ru.solrudev.ackpine.plugability.UninstallPluginScope
import ru.solrudev.ackpine.uninstaller.parameters.UninstallerType

@JvmSynthetic
internal fun resolveUninstallerCapabilities(
	requestedType: UninstallerType,
	pluginClasses: List<Class<out AckpineUninstallPlugin<*>>>
): UninstallerCapabilities {
	val sdkInt = SdkIntWrapper.get()
	val scope = UninstallPluginScope.create(requestedType)
	pluginClasses.forEach(scope::registerCapabilityPlugin)
	scope.resolvePlugins(::normalizeUninstallerType)
	val finalType = scope.uninstallerType
	val context = UninstallCapabilityContext(sdkInt, finalType)
	return UninstallerCapabilities(
		uninstallerType = finalType,
		pluginCapabilities = collectUninstallPluginCapabilities(scope, context)
	)
}

private fun collectUninstallPluginCapabilities(
	scope: UninstallPluginScope,
	context: UninstallCapabilityContext
): Map<Class<out UninstallCapabilityProvider<*>>, PluginCapability> {
	val result = mutableMapOf<Class<out UninstallCapabilityProvider<*>>, PluginCapability>()
	for (pluginClass in scope.getPlugins().keys) {
		val plugin = AckpinePluginCache.get(pluginClass)
		if (plugin is UninstallCapabilityProvider<*>) {
			@Suppress("UNCHECKED_CAST")
			result[pluginClass as Class<UninstallCapabilityProvider<*>>] = plugin.getCapabilities(context)
		}
	}
	return result
}

private fun normalizeUninstallerType(type: UninstallerType): UninstallerType {
	if (!isPackageInstallerApiAvailable()) {
		return UninstallerType.INTENT_BASED
	}
	return type
}