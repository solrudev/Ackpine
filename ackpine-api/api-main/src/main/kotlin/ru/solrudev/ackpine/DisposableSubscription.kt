/*
 * Copyright (C) 2023 Ilya Fomichev
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

package ru.solrudev.ackpine

import androidx.annotation.RestrictTo
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A handle to a subscription via listener which can be disposed.
 */
public interface DisposableSubscription {

	/**
	 * Returns whether this subscription was disposed.
	 */
	public val isDisposed: Boolean

	/**
	 * Disposes this subscription and removes the listener from publisher.
	 */
	public fun dispose()
}

/**
 * A container for multiple [disposable subscriptions][DisposableSubscription].
 */
public class DisposableSubscriptionContainer : DisposableSubscription {

	override val isDisposed: Boolean
		get() = _isDisposed.get()

	private val _isDisposed = AtomicBoolean(false)

	private val subscriptions = Collections.newSetFromMap(
		ConcurrentHashMap<DisposableSubscription, Boolean>()
	)

	/**
	 * Adds the specified [subscription] to this [DisposableSubscriptionContainer] if it's not added yet.
	 *
	 * If this container is already [disposed][isDisposed], the [subscription] is disposed immediately.
	 */
	public fun add(subscription: DisposableSubscription) {
		if (subscription == DummyDisposableSubscription) {
			return
		}
		if (isDisposed) {
			subscription.dispose()
			return
		}
		subscriptions += subscription
		if (isDisposed && subscriptions.remove(subscription)) {
			subscription.dispose()
		}
	}

	/**
	 * Clears this [DisposableSubscriptionContainer] disposing contained subscriptions, without disposing the container
	 * itself.
	 */
	public fun clear() {
		for (subscription in subscriptions) {
			subscription.dispose()
		}
		subscriptions.clear()
	}

	public override fun dispose() {
		if (_isDisposed.compareAndSet(false, true)) {
			clear()
		}
	}
}

/**
 * No-op [DisposableSubscription].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public data object DummyDisposableSubscription : DisposableSubscription {
	override val isDisposed: Boolean = true
	override fun dispose() { /* no-op */ }
}