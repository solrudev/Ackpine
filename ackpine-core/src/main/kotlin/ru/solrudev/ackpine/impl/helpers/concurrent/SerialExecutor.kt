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

package ru.solrudev.ackpine.impl.helpers.concurrent

import androidx.annotation.RestrictTo
import java.util.concurrent.Executor

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class SerialExecutor internal constructor(private val executor: Executor) : Executor {

	private val lock = Any()
	private val tasks = ArrayDeque<Task>()
	private var activeCommand: Runnable? = null

	override fun execute(command: Runnable) = synchronized(lock) {
		tasks += Task(this, command)
		if (activeCommand == null) {
			scheduleNext()
		}
	}

	private fun scheduleNext() {
		if (tasks.removeFirstOrNull().also { activeCommand = it } != null) {
			try {
				executor.execute(activeCommand)
			} catch (throwable: Throwable) {
				activeCommand = null
				throw throwable
			}
		}
	}

	private class Task(private val serialExecutor: SerialExecutor, private val runnable: Runnable) : Runnable {

		override fun run() = try {
			runnable.run()
		} finally {
			synchronized(serialExecutor.lock) {
				serialExecutor.scheduleNext()
			}
		}
	}
}