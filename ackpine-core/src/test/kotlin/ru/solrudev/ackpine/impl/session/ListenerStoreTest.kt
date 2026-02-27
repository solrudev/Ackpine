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

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ListenerStoreTest {

	@Test
	fun addReturnsNullForDuplicateListener() {
		val store = ListenerStore<TestListener>()
		val first = store.add(TestListener(1))
		val duplicate = store.add(TestListener(1))

		assertNotNull(first)
		assertNull(duplicate)
		assertTrue(store.isValid(first))
	}

	@Test
	fun removeByListenerDeactivatesCurrentRegistration() {
		val store = ListenerStore<TestListener>()
		val listener = TestListener(2)
		val registration = assertNotNull(store.add(listener))

		assertTrue(store.remove(listener))
		assertFalse(registration.isActive)
		assertFalse(store.isValid(registration))
	}

	@Test
	fun removeAndReaddBehavior() {
		val store = ListenerStore<TestListener>()
		val listener = TestListener(3)
		val oldRegistration = assertNotNull(store.add(listener))
		store.remove(listener)
		val newRegistration = assertNotNull(store.add(TestListener(3)))

		assertFalse(oldRegistration.isActive)
		assertFalse(store.remove(oldRegistration))
		assertFalse(store.isValid(oldRegistration))
		assertTrue(store.isValid(newRegistration))
	}

	@Test
	fun subscriptionRemovesRegistration() {
		val store = ListenerStore<TestListener>()
		val listener = TestListener(5)
		val registration = assertNotNull(store.add(listener))
		val disposable = store.subscriptionOf(registration)

		disposable.dispose()
		disposable.dispose()

		assertTrue(disposable.isDisposed)
		assertFalse(store.isValid(registration))
		assertFalse(store.remove(listener))
	}

	private data class TestListener(val id: Int)
}