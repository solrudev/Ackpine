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
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DisposableSubscriptionContainerTest {

	@Test
	fun addOnDisposedContainerDisposesIncomingSubscription() {
		val container = DisposableSubscriptionContainer()
		container.dispose()
		val subscription = TestSubscription()

		container.add(subscription)

		assertTrue(subscription.isDisposed)
	}

	@Test
	fun addOnActiveContainerRetainsSubscriptionUntilDispose() {
		val container = DisposableSubscriptionContainer()
		val subscription = TestSubscription()

		container.add(subscription)
		assertFalse(subscription.isDisposed)

		container.dispose()

		assertTrue(subscription.isDisposed)
	}

	@Test
	fun clearDisposesSubscriptionsButContainerRemainsActive() {
		val container = DisposableSubscriptionContainer()
		val subscription = TestSubscription()
		container.add(subscription)

		container.clear()

		assertTrue(subscription.isDisposed)
		assertFalse(container.isDisposed)

		val newSubscription = TestSubscription()
		container.add(newSubscription)
		assertFalse(newSubscription.isDisposed)
	}

	@Test
	fun disposeIsIdempotent() {
		val container = DisposableSubscriptionContainer()
		val subscription = TestSubscription()
		container.add(subscription)

		container.dispose()
		container.dispose()

		assertEquals(1, subscription.disposeCount)
	}

	private class TestSubscription : DisposableSubscription {

		var disposeCount = 0
			private set

		override val isDisposed: Boolean
			get() = disposeCount > 0

		override fun dispose() {
			disposeCount++
		}
	}
}