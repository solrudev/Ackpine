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

import androidx.annotation.RestrictTo

/**
 * A container of [AckpinePlugins][AckpinePlugin] applied to a session.
 */
public class AckpinePluginContainer private constructor(
	private val pluginsMap: Map<Class<out AckpinePlugin<*>>, AckpinePlugin.Parameters>
) {

	private val pluginInstancesMap by lazy {
		pluginsMap.mapKeys { (pluginClass, _) -> AckpinePluginCache.get(pluginClass) }
	}

	/**
	 * Returns a new copy of plugin entries saved inside of this container.
	 */
	public fun getPlugins(): Map<Class<out AckpinePlugin<*>>, AckpinePlugin.Parameters> {
		return pluginsMap.toMap()
	}

	/**
	 * Returns a map of plugin instances with their parameters saved inside of this container.
	 */
	@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
	@JvmSynthetic
	public fun getPluginInstances(): Map<AckpinePlugin<*>, AckpinePlugin.Parameters> {
		return pluginInstancesMap.toMap()
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is AckpinePluginContainer) return false
		return pluginsMap == other.pluginsMap
	}

	override fun hashCode(): Int = pluginsMap.hashCode()
	override fun toString(): String = "AckpinePluginContainer($pluginsMap)"

	internal companion object {

		@JvmSynthetic
		internal fun from(
			plugins: Map<Class<out AckpinePlugin<*>>, AckpinePlugin.Parameters>
		) = AckpinePluginContainer(plugins.filterNot { it.key.name == AckpinePlugin::class.java.name })
	}
}