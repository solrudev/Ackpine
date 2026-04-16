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
import ru.solrudev.ackpine.impl.logging.AckpineLoggerProvider
import ru.solrudev.ackpine.impl.session.CompletableSession
import ru.solrudev.ackpine.plugability.AckpinePlugin
import ru.solrudev.ackpine.plugability.AckpinePluginCache
import ru.solrudev.ackpine.plugability.AckpinePluginContainer
import java.util.ServiceLoader
import java.util.UUID
import kotlin.reflect.KClass

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class AckpineServiceProviders(
	private val serviceProviders: Lazy<Set<AckpineServiceProvider>>,
	private val logger: AckpineLoggerProvider
) {

	@JvmSynthetic
	internal fun getAll() = serviceProviders.value

	@JvmSynthetic
	internal fun getByPlugins(pluginClasses: Collection<Class<out AckpinePlugin>>): List<AckpineServiceProvider> {
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
		pluginClasses: Result<Collection<Class<out AckpinePlugin>>>,
		serviceProviders: Result<List<AckpineServiceProvider>> = pluginClasses.mapCatching(::getByPlugins),
		pluginParameters: Result<Collection<AckpinePlugin.Parameters>> = serviceProviders.mapCatching { providers ->
			providers.flatMap { serviceProvider ->
				serviceProvider
					.getPluginParametersStores()
					.map { parametersStore -> parametersStore.getForSession(sessionId) }
			}
		},
		sessionFactory: (Lazy<S>) -> R
	): R {
		val service = serviceProviders.mapCatching { providers ->
			if (providers.isEmpty()) {
				logger.debug(
					"Using default %s for session %s",
					serviceClass.qualifiedName,
					sessionId
				)
				return sessionFactory(defaultService)
			}
			resolveService(providers, serviceClass, sessionId, pluginParameters)
		}
		val session = sessionFactory(service.getOrNull() ?: defaultService)
		service.onFailure { throwable ->
			logger.error(
				throwable,
				"Failed to resolve %s for session %s",
				serviceClass.qualifiedName,
				sessionId
			)
			when (throwable) {
				is Error -> session.completeExceptionally(RuntimeException(throwable))
				is Exception -> session.completeExceptionally(throwable)
			}
		}
		return session
	}

	private fun <S : AckpineService> resolveService(
		providers: List<AckpineServiceProvider>,
		serviceClass: KClass<S>,
		sessionId: UUID,
		pluginParameters: Result<Collection<AckpinePlugin.Parameters>>
	): AckpineServiceLazy<S>? {
		val resolvedService = providers.firstNotNullOfOrNull { provider ->
			provider.getLazy(serviceClass)?.let { service ->
				ResolvedAckpineService(provider, service)
			}
		}
		if (resolvedService != null) {
			logger.debug(
				"Selected service %s for session %s provider=%s",
				serviceClass.qualifiedName,
				sessionId,
				resolvedService.provider::class.java.name
			)
			pluginParameters
				.getOrThrow()
				.filterNot { params -> params == AckpinePlugin.Parameters.None }
				.forEach { params -> resolvedService.service.applyParameters(sessionId, params) }
			return resolvedService.service
		}
		logger.debug(
			"Using default %s for session %s because no provider exposed it",
			serviceClass.qualifiedName,
			sessionId
		)
		return null
	}

	internal companion object Factory {

		private const val TAG = "AckpineServiceProviders"

		@JvmSynthetic
		internal fun create(
			context: Context,
			loggerProvider: AckpineLoggerProvider
		): AckpineServiceProviders {
			val logger = loggerProvider.withTag(TAG)
			return AckpineServiceProviders(
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
						.also { providers ->
							logger.debug("Discovered service providers=%s", providers.map { it::class.java.name })
						}
				},
				logger
			)
		}
	}
}

private class ResolvedAckpineService<S : AckpineService>(
	val provider: AckpineServiceProvider,
	val service: AckpineServiceLazy<S>
)