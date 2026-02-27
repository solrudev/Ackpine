/*
 * Copyright (C) 2026 Ilya Fomichev
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

package ru.solrudev.ackpine.impl.session

import androidx.annotation.RestrictTo
import ru.solrudev.ackpine.DisposableSubscription
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class ListenerStore<L : Any> {

	private val registrations = ConcurrentHashMap<L, Registration<L>>()

	@JvmSynthetic
	internal fun add(listener: L): Registration<L>? {
		val registration = Registration(listener)
		if (registrations.putIfAbsent(listener, registration) == null) {
			return registration
		}
		return null
	}

	@JvmSynthetic
	internal fun remove(listener: L): Boolean {
		val registration = registrations.remove(listener) ?: return false
		registration.deactivate()
		return true
	}

	@JvmSynthetic
	internal fun remove(registration: Registration<L>): Boolean {
		if (!registrations.remove(registration.listener, registration)) {
			return false
		}
		registration.deactivate()
		return true
	}

	@JvmSynthetic
	internal fun isValid(registration: Registration<L>): Boolean {
		return registration.isActive && registrations[registration.listener] === registration
	}

	@JvmSynthetic
	internal inline fun forEach(block: (Registration<L>) -> Unit) {
		for (registration in registrations.values) {
			block(registration)
		}
	}

	@JvmSynthetic
	internal fun subscriptionOf(registration: Registration<L>): DisposableSubscription {
		return ListenerSubscription(this, registration)
	}

	@RestrictTo(RestrictTo.Scope.LIBRARY)
	internal class Registration<L : Any> internal constructor(val listener: L) {

		@Volatile
		private var _isActive = true

		@get:JvmSynthetic
		internal val isActive: Boolean
			get() = _isActive

		@JvmSynthetic
		internal fun deactivate() {
			_isActive = false
		}
	}
}

private class ListenerSubscription<L : Any>(
	store: ListenerStore<L>,
	registration: ListenerStore.Registration<L>
) : DisposableSubscription {

	override val isDisposed: Boolean
		get() = _isDisposed.get()

	private val store = WeakReference(store)
	private val registration = WeakReference(registration)
	private val _isDisposed = AtomicBoolean(false)

	override fun dispose() {
		if (!_isDisposed.compareAndSet(false, true)) {
			return
		}
		val registration = registration.get()
		if (registration != null) {
			store.get()?.remove(registration)
		}
		this.registration.clear()
		this.store.clear()
	}
}