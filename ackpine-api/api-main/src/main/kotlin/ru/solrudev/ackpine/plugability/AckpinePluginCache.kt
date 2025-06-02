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
import java.util.concurrent.ConcurrentHashMap

/**
 * Cache of [AckpinePlugins][AckpinePlugin].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object AckpinePluginCache {

	private val plugins = ConcurrentHashMap<Class<out AckpinePlugin<*>>, AckpinePlugin<*>>()

	/**
	 * Returns plugin for the given [pluginClass]. If it's not found in the cache, creates a new instance of the plugin
	 * of the [pluginClass] type, puts its result into the cache and returns it.
	 */
	@Suppress("UNCHECKED_CAST")
	@JvmSynthetic
	public fun <T : AckpinePlugin<*>> get(pluginClass: Class<T>): T = plugins.getOrPut(pluginClass) {
		pluginClass
			.getDeclaredConstructor()
			.apply { isAccessible = true }
			.newInstance()
	} as T
}