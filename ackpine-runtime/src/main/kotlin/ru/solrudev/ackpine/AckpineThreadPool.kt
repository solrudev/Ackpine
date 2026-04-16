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

package ru.solrudev.ackpine

import androidx.annotation.RestrictTo
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.roundToInt

private val ACKPINE_THREAD_POOL_SIZE = (Runtime.getRuntime().availableProcessors() * 1.8).roundToInt()

/**
 * A thread pool shared between all Ackpine modules.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object AckpineThreadPool : ExecutorService by Executors.newFixedThreadPool(
	ACKPINE_THREAD_POOL_SIZE,
	object : ThreadFactory {
		private val threadCounter = AtomicInteger(0)

		override fun newThread(runnable: Runnable?): Thread {
			return Thread(runnable, "ackpine.pool-${threadCounter.incrementAndGet()}")
		}
	}
) {

	@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
	public val threadCount: Int = ACKPINE_THREAD_POOL_SIZE
}