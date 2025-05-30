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

public class AckpinePluginContainer private constructor(
	private val pluginsMap: Map<Class<out AckpinePlugin>, AckpinePlugin.Parameters<AckpinePlugin>>
) {

	private val plugins by lazy {
		pluginsMap.map { (pluginClass, params) ->
			pluginClass
				.getDeclaredConstructor()
				.apply { isAccessible = true }
				.newInstance() as AckpinePlugin to params
		}
	}

	public fun getPlugins(): Set<Entry> = plugins
		.map { (plugin, params) ->
			Entry(plugin, params)
		}
		.toSet()

	public fun getPluginClasses(): Map<Class<out AckpinePlugin>, AckpinePlugin.Parameters<AckpinePlugin>> {
		return pluginsMap.toMap()
	}

	public data class Entry(
		public val plugin: AckpinePlugin,
		public val parameters: AckpinePlugin.Parameters<*>
	)

	internal companion object {
		@JvmSynthetic
		internal fun from(
			plugins: Map<Class<out AckpinePlugin>, AckpinePlugin.Parameters<AckpinePlugin>>
		) = AckpinePluginContainer(plugins.filterNot { it.key == AckpinePlugin::class.java })
	}
}