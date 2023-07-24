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

	override var isDisposed: Boolean = false
		private set

	private val subscriptions = mutableListOf<DisposableSubscription>()

	/**
	 * Adds the specified [subscription] to this [DisposableSubscriptionContainer].
	 */
	public fun add(subscription: DisposableSubscription) {
		if (!isDisposed) {
			subscriptions += subscription
		}
	}

	/**
	 * Clears this [DisposableSubscriptionContainer] disposing contained subscriptions, without disposing the container
	 * itself.
	 */
	public fun clear() {
		subscriptions.forEach { it.dispose() }
		subscriptions.clear()
	}

	public override fun dispose() {
		if (!isDisposed) {
			clear()
			isDisposed = true
		}
	}
}