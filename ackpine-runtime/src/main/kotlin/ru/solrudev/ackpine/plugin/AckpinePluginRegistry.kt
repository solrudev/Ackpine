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

package ru.solrudev.ackpine.plugin

import androidx.annotation.RestrictTo
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.roundToInt

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object AckpinePluginRegistry {

	private val executor = Executors.newFixedThreadPool(
		(Runtime.getRuntime().availableProcessors() * 1.8).roundToInt(),
		object : ThreadFactory {
			private val threadCount = AtomicInteger(0)

			override fun newThread(runnable: Runnable?): Thread {
				return Thread(runnable, "ackpine.pool-${threadCount.incrementAndGet()}")
			}
		}
	)

	public fun register(plugin: AckpinePlugin) {
		plugin.setExecutor(executor)
	}
}