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
import androidx.annotation.WorkerThread
import ru.solrudev.ackpine.impl.session.CompletableSession
import ru.solrudev.ackpine.plugability.AckpinePlugin
import ru.solrudev.ackpine.plugability.AckpinePluginCache
import ru.solrudev.ackpine.plugability.AckpinePluginContainer
import java.util.ServiceLoader
import java.util.UUID
import kotlin.reflect.KClass

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class AckpineServiceProviders(private val serviceProviders: Lazy<Set<AckpineServiceProvider>>) {

	@JvmSynthetic
	internal fun getAll() = serviceProviders.value

	@JvmSynthetic
	internal fun getByPlugins(pluginClasses: Collection<Class<out AckpinePlugin<*>>>): List<AckpineServiceProvider> {
		val appliedPlugins = pluginClasses.mapTo(mutableSetOf()) { pluginClass ->
			AckpinePluginCache.get(pluginClass).id
		}
		return getAll().filter { provider ->
			provider.pluginIdentifiers.any { it in appliedPlugins }
		}
	}

	@WorkerThread
	@JvmSynthetic
	internal fun persistPluginParameters(sessionId: UUID, pluginContainer: AckpinePluginContainer) {
		val plugins = pluginContainer.getPlugins()
		val pluginParams = plugins.values
		getByPlugins(plugins.keys)
			.flatMap { serviceProvider ->
				serviceProvider.getPluginParametersStores()
			}
			.forEach { parametersStore ->
				for (params in pluginParams) {
					parametersStore.setForSession(sessionId, params)
				}
			}
	}

	@JvmSynthetic
	internal fun <S : AckpineService, R : CompletableSession<*>> createSessionWithService(
		serviceClass: KClass<S>,
		defaultService: Lazy<S>,
		sessionId: UUID,
		pluginClasses: Result<Collection<Class<out AckpinePlugin<*>>>>,
		serviceProviders: Result<List<AckpineServiceProvider>> = pluginClasses.mapCatching(::getByPlugins),
		pluginParameters: Result<Collection<AckpinePlugin.Parameters>> = serviceProviders.mapCatching { providers ->
			providers.flatMap { serviceProvider ->
				serviceProvider
					.getPluginParametersStores()
					.map { parametersStore -> parametersStore.getForSession(sessionId) }
			}
		},
		sessionFactory: (S) -> R
	): R {
		val service = serviceProviders.mapCatching { providers ->
			if (providers.isEmpty()) {
				return sessionFactory(defaultService.value)
			}
			providers
				.firstNotNullOfOrNull { provider -> provider[serviceClass] }
				?.also { service ->
					pluginParameters
						.getOrThrow()
						.filterNot { params -> params == AckpinePlugin.Parameters.None }
						.forEach { params -> service.applyParameters(sessionId, params) }
				}
		}
		val session = sessionFactory(service.getOrNull() ?: defaultService.value)
		service.onFailure { throwable ->
			when (throwable) {
				is Error -> session.completeExceptionally(RuntimeException(throwable))
				is Exception -> session.completeExceptionally(throwable)
			}
		}
		return session
	}

	internal companion object Factory {

		@JvmSynthetic
		internal fun create(context: Context) = AckpineServiceProviders(
			serviceProviders = lazy {
				ServiceLoader
					.load(
						AckpineServiceProvider::class.java,
						AckpineServiceProvider::class.java.classLoader
					)
					.iterator()
					.asSequence()
					.onEach { provider -> provider.initContext(context) }
					.toSet()
			}
		)
	}
}