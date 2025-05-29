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

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface AckpineServiceProvider {
	public val plugin: AckpinePlugin
	public fun <T : Any> get(serviceClass: KClass<T>, context: Context): T?
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class AbstractAckpineServiceProvider(
	serviceFactories: Set<ServiceFactory<*>>
) : AckpineServiceProvider {

	private val factories = serviceFactories.associate { it.serviceClass to it.serviceFactory }
	private val services = ConcurrentHashMap<KClass<*>, Any>()

	@Suppress("UNCHECKED_CAST")
	override fun <T : Any> get(serviceClass: KClass<T>, context: Context): T? {
		if (serviceClass !in factories.keys) {
			return null
		}
		return services.computeIfAbsentCompat(serviceClass) {
			factories.getValue(serviceClass).invoke(context)
		} as? T
	}

	@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
	public class ServiceFactory<T : Any>(
		public val serviceClass: KClass<T>,
		public val serviceFactory: (Context) -> T
	)
}

@JvmSynthetic
internal inline fun <reified T : Any> AckpineServiceProvider.get(context: Context): T? = get(T::class, context)