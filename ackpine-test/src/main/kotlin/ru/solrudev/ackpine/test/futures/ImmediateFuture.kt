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

import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

/**
 * A [ListenableFuture] that is already completed.
 *
 * Listeners run immediately on the provided executor, and cancellation is not supported. This is intended for
 * deterministic unit tests.
 */
public class ImmediateFuture<V> private constructor(
	private val value: V?,
	private val throwable: Throwable?
) : ListenableFuture<V> {

	override fun addListener(listener: Runnable, executor: Executor) {
		executor.execute(listener)
	}

	override fun cancel(mayInterruptIfRunning: Boolean): Boolean = false

	override fun isCancelled(): Boolean = false

	override fun isDone(): Boolean = true

	override fun get(): V {
		if (throwable != null) {
			throw ExecutionException(throwable)
		}
		@Suppress("UNCHECKED_CAST")
		return value as V
	}

	override fun get(timeout: Long, unit: TimeUnit): V {
		return get()
	}

	public companion object {

		/**
		 * Creates a successful [ImmediateFuture] from the provided [value].
		 */
		@JvmStatic
		public fun <T> success(value: T): ImmediateFuture<T> = ImmediateFuture(value, null)

		/**
		 * Creates a failed [ImmediateFuture] from the provided [throwable].
		 */
		@JvmStatic
		public fun <T> failure(throwable: Throwable): ImmediateFuture<T> = ImmediateFuture(null, throwable)
	}
}