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

package ru.solrudev.ackpine.test.futures

import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class ImmediateFutureTest {

	@Test
	fun successValueReturnedByGetAndTimedGet() {
		val future = ImmediateFuture.success("value")
		assertEquals("value", future.get())
		assertEquals("value", future.get(1, TimeUnit.SECONDS))
	}

	@Test
	fun failureWrapsCauseInExecutionException() {
		val cause = IllegalArgumentException("boom")
		val future = ImmediateFuture.failure<Nothing>(cause)

		val fromGet = assertFailsWith<ExecutionException> { future.get() }
		val fromTimedGet = assertFailsWith<ExecutionException> { future.get(1, TimeUnit.MILLISECONDS) }

		assertSame(cause, fromGet.cause)
		assertSame(cause, fromTimedGet.cause)
	}

	@Test
	fun addListenerExecutesImmediatelyOnProvidedExecutor() {
		val executed = AtomicBoolean(false)
		val invocations = AtomicInteger(0)
		val future = ImmediateFuture.success(1)

		future.addListener(
			listener = { executed.set(true) },
			executor = { command ->
				invocations.incrementAndGet()
				command.run()
			}
		)

		assertTrue(executed.get())
		assertEquals(1, invocations.get())
	}

	@Test
	fun cancelIsNoOp() {
		val future = ImmediateFuture.success(1)
		assertFalse(future.cancel(true))
		assertFalse(future.isCancelled)
		assertTrue(future.isDone)
	}
}