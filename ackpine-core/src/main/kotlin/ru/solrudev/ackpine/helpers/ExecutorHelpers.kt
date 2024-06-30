/*
 * Copyright (C) 2023-2024 Ilya Fomichev
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

package ru.solrudev.ackpine.helpers

import android.annotation.SuppressLint
import androidx.concurrent.futures.ResolvableFuture
import java.util.concurrent.Executor
import java.util.concurrent.Semaphore

@SuppressLint("RestrictedApi")
@JvmSynthetic
internal inline fun <V> Executor.executeWithFuture(future: ResolvableFuture<V>, crossinline command: () -> Unit) {
	try {
		execute {
			try {
				command()
			} catch (t: Throwable) {
				future.setException(t)
			}
		}
	} catch (throwable: Throwable) {
		future.setException(throwable)
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