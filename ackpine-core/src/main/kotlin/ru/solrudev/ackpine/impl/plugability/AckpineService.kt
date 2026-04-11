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
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * A service provided by an [AckpinePlugin] via [AckpineServiceProvider].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface AckpineService {

	/**
	 * Applies [parameters] of an [AckpinePlugin] to a session with ID equal to [sessionId].
	 */
	public fun applyParameters(sessionId: UUID, parameters: AckpinePlugin.Parameters)
}

/**
 * A lazy instance of [AckpineService], allowing to register parameters before creating it.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class AckpineServiceLazy<T : AckpineService>(factory: () -> T) : Lazy<T>, AckpineService {

	override val value: T
		get() = service.value

	private val lazyParameters = ConcurrentLinkedQueue<Pair<UUID, AckpinePlugin.Parameters>>()

	private val service = lazy {
		val service = factory()
		for ((sessionId, parameters) in lazyParameters) {
			service.applyParameters(sessionId, parameters)
		}
		lazyParameters.clear()
		service
	}

	override fun isInitialized(): Boolean = service.isInitialized()

	override fun applyParameters(sessionId: UUID, parameters: AckpinePlugin.Parameters) {
		if (service.isInitialized()) {
			service.value.applyParameters(sessionId, parameters)
			return
		}
		lazyParameters += sessionId to parameters
	}
}