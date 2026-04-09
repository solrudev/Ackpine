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
import kotlin.reflect.KClass

/**
 * Returns the plugin-specific uninstall capabilities for the given [pluginClass], or `null` if the plugin was not in
 * the resolved graph.
 * @param pluginClass Kotlin class of the plugin implementing both [AckpineUninstallPlugin] and
 * [UninstallCapabilityProvider].
 * @see UninstallerCapabilities.plugin
 */
public fun <Plugin, C> UninstallerCapabilities.plugin(pluginClass: KClass<out Plugin>): C?
		where Plugin : AckpineUninstallPlugin<*>,
			  Plugin : UninstallCapabilityProvider<C>,
			  C : PluginCapability {
	return plugin(pluginClass.java)
}