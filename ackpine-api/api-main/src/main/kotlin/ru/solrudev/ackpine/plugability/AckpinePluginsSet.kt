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

public class AckpinePluginsSet private constructor(private val pluginClasses: Set<Class<out AckpinePlugin>>) {

	private val plugins by lazy {
		pluginClasses.map {
			pluginClass -> pluginClass
				.getDeclaredConstructor()
				.apply { isAccessible = true }
				.newInstance() as AckpinePlugin
		}
	}

	public fun toPluginsSet(): Set<AckpinePlugin> = plugins.toSet()
	public fun toPluginClassesSet(): Set<Class<out AckpinePlugin>> = pluginClasses.toSet()

	internal companion object {
		@JvmSynthetic
		internal fun from(pluginClasses: Set<Class<out AckpinePlugin>>) = AckpinePluginsSet(
			pluginClasses
				.filterNot { it == AckpinePlugin::class.java }
				.toSet()
		)
	}
}