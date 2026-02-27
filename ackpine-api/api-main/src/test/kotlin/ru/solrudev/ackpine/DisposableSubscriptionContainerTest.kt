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

package ru.solrudev.ackpine

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DisposableSubscriptionContainerTest {

	@Test
	fun addOnDisposedContainerDisposesIncomingSubscription() {
		val container = DisposableSubscriptionContainer()
		container.dispose()
		val subscription = Subscription()

		container.add(subscription)

		assertTrue(subscription.isDisposed)
	}

	@Test
	fun addOnActiveContainerRetainsSubscriptionUntilDispose() {
		val container = DisposableSubscriptionContainer()
		val subscription = Subscription()

		container.add(subscription)
		assertFalse(subscription.isDisposed)

		container.dispose()

		assertTrue(subscription.isDisposed)
	}

	private class Subscription : DisposableSubscription {

		override var isDisposed = false
			private set

		override fun dispose() {
			isDisposed = true
		}
	}
}