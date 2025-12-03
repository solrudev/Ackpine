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

import android.content.Context
import androidx.annotation.RestrictTo
import ru.solrudev.ackpine.impl.helpers.concurrent.computeIfAbsentCompat
import ru.solrudev.ackpine.plugability.AckpinePlugin
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * Provider for an [AckpineService].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface AckpineServiceProvider {

	/**
	 * IDs of the [AckpinePlugins][AckpinePlugin] to which this provider belongs.
	 */
	public val pluginIdentifiers: Set<String>

	/**
	 * Returns stores for managing [AckpinePlugin parameters][AckpinePlugin.Parameters].
	 */
	public fun getPluginParametersStores(): List<PluginParametersStore>

	/**
	 * Initializes this service provider with provided [Context].
	 */
	public fun initContext(context: Context)

	/**
	 * Returns an [AckpineService] of the given [class][serviceClass]. May return `null` if this provider doesn't hold
	 * the requested service or if the requested service is not supported.
	 * @param serviceClass a Kotlin class of the requested service.
	 */
	public operator fun <T : AckpineService> get(serviceClass: KClass<T>): T?
}

/**
 * Base implementation for [AckpineServiceProvider]. Implements lazy and thread-safe creation of the singleton
 * [services][AckpineService].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class AbstractAckpineServiceProvider(
	serviceFactories: Set<ServiceFactory<*>>,
	pluginEntries: Set<PluginEntry>
) : AckpineServiceProvider {

	override val pluginIdentifiers: Set<String> = pluginEntries
		.map { pluginEntry -> pluginEntry.pluginId }
		.toSet()

	private val pluginParametersStoreFactories = pluginEntries.associate {
		it.pluginId to it.pluginParametersStoreFactory
	}

	private val factories = serviceFactories.associate { it.serviceClass to it.serviceFactory }
	private val pluginParameters = ConcurrentHashMap<String, PluginParametersStore>()
	private val services = ConcurrentHashMap<KClass<out AckpineService>, AckpineService>()
	private lateinit var context: Context

	override fun initContext(context: Context) {
		this.context = context.applicationContext
	}

	@Suppress("UNCHECKED_CAST")
	override fun <T : AckpineService> get(serviceClass: KClass<T>): T? {
		if (serviceClass !in factories.keys) {
			return null
		}
		return services.computeIfAbsentCompat(serviceClass) {
			factories.getValue(serviceClass).invoke(context)
		} as? T
	}

	override fun getPluginParametersStores(): List<PluginParametersStore> {
		return pluginIdentifiers.mapNotNull { pluginId ->
			pluginParameters.computeIfAbsentCompat(pluginId) {
				pluginParametersStoreFactories[pluginId]?.invoke(context)
			}
		}
	}

	/**
	 * Entry for the [AbstractAckpineServiceProvider] set of service factories.
	 * @property serviceClass Kotlin class of the created service.
	 * @property serviceFactory function which creates service of the [serviceClass] type.
	 */
	@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
	public class ServiceFactory<T : AckpineService>(
		public val serviceClass: KClass<T>,
		public val serviceFactory: (Context) -> T
	)

	/**
	 * Entry for the [AbstractAckpineServiceProvider] set of plugins which own this provider.
	 * @property pluginId ID of the plugin.
	 * @property pluginParametersStoreFactory function which creates [PluginParametersStore] for the plugin.
	 */
	@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
	public class PluginEntry(
		public val pluginId: String,
		public val pluginParametersStoreFactory: (Context) -> PluginParametersStore = { EmptyPluginParametersStore }
	)
}