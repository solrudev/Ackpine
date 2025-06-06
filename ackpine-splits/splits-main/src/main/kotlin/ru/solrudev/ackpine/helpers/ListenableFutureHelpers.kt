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

package ru.solrudev.ackpine.helpers

import android.annotation.SuppressLint
import androidx.annotation.RestrictTo
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.concurrent.futures.DirectExecutor
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

@SuppressLint("RestrictedApi")
@JvmSynthetic
internal fun <V> CallbackToFutureAdapter.Completer<V>.onCancellation(block: () -> Unit) {
	addCancellationListener(block, DirectExecutor.INSTANCE)
}

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class ImmediateListenableFuture<V>(private val value: V) : ListenableFuture<V> {
	override fun cancel(mayInterruptIfRunning: Boolean) = false
	override fun isCancelled() = false
	override fun isDone() = true
	override fun get() = value
	override fun get(timeout: Long, unit: TimeUnit?) = value
	override fun addListener(listener: Runnable, executor: Executor) = listener.run()
}