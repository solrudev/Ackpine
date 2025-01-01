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

package ru.solrudev.ackpine.helpers.concurrent

import androidx.concurrent.futures.CallbackToFutureAdapter.Completer
import java.util.concurrent.Executor
import java.util.concurrent.Semaphore

@JvmSynthetic
internal inline fun <V> Executor.executeWithCompleter(completer: Completer<V>, crossinline command: () -> Unit) {
	try {
		execute {
			try {
				command()
			} catch (t: Throwable) {
				completer.setException(t)
			}
		}
	} catch (throwable: Throwable) {
		completer.setException(throwable)
	}
}

@JvmSynthetic
internal inline fun Executor.executeWithSemaphore(semaphore: Semaphore, crossinline command: () -> Unit) {
	semaphore.acquire()
	try {
		execute {
			try {
				command()
			} finally {
				semaphore.release()
			}
		}
	} catch (exception: Exception) {
		semaphore.release()
		throw exception
	}
}