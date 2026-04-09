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

/**
 * Optional interface for [AckpineUninstallPlugins][AckpineUninstallPlugin] that want to expose plugin-specific
 * uninstall capability information via [UninstallerCapabilities.plugin].
 *
 * After the full plugin graph has been resolved, each plugin in the graph that implements this interface will be
 * queried via [getCapabilities] and its result stored, keyed by the plugin class. The result can then be retrieved
 * by consumers via `UninstallerCapabilities.plugin(YourPlugin::class.java)`.
 */
public interface UninstallCapabilityProvider<C : PluginCapability> {

	/**
	 * Returns plugin-specific uninstall capability information for the given [context].
	 */
	public fun getCapabilities(context: UninstallCapabilityContext): C
}