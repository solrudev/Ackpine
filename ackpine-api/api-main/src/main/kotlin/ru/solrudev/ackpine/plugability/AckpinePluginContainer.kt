/*
 * Copyright (C) 2025 Ilya Fomichev
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

/**
 * A container of [AckpinePlugins][AckpinePlugin] applied to a session.
 */
public class AckpinePluginContainer private constructor(
	private val pluginsMap: Map<Class<out AckpinePlugin<*>>, AckpinePlugin.Parameters>
) {

	private val plugins by lazy {
		pluginsMap.mapKeys { (pluginClass, _) -> AckpinePluginCache.get(pluginClass) }
	}

	/**
	 * Returns a new copy of plugin [entries][Entry] saved inside of this container.
	 */
	public fun getPlugins(): Set<Entry> {
		val set = mutableSetOf<Entry>()
		return plugins.mapTo(set) { (plugin, params) -> Entry(plugin, params) }
	}

	/**
	 * Returns a map of plugin classes with their parameters saved inside of this container.
	 */
	public fun getPluginClasses(): Map<Class<out AckpinePlugin<*>>, AckpinePlugin.Parameters> {
		return pluginsMap.toMap()
	}

	/**
	 * An [AckpinePluginContainer] entry.
	 * @property plugin an instance of [AckpinePlugin].
	 * @property parameters a set of parameters for the [plugin].
	 */
	public data class Entry(
		public val plugin: AckpinePlugin<*>,
		public val parameters: AckpinePlugin.Parameters
	)

	internal companion object {
		@JvmSynthetic
		internal fun from(
			plugins: Map<Class<out AckpinePlugin<*>>, AckpinePlugin.Parameters>
		) = AckpinePluginContainer(plugins.filterNot { it.key == AckpinePlugin::class.java })
	}
}