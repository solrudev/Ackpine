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

package ru.solrudev.ackpine.impl.plugability

import androidx.annotation.RestrictTo
import ru.solrudev.ackpine.plugability.AckpinePlugin
import ru.solrudev.ackpine.plugability.AckpinePluginCache

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class AckpineServiceProviders(private val serviceProviders: Lazy<Set<AckpineServiceProvider>>) {

	@JvmSynthetic
	internal fun getAll() = serviceProviders.value

	@JvmSynthetic
	internal fun getByPlugins(pluginClasses: Collection<Class<out AckpinePlugin<*>>>): List<AckpineServiceProvider> {
		val plugins = pluginClasses.map { pluginClass -> AckpinePluginCache.get(pluginClass) }
		val appliedPlugins = plugins.map { plugin -> plugin.id }
		return getAll().filter { provider -> provider.pluginId in appliedPlugins }
	}
}