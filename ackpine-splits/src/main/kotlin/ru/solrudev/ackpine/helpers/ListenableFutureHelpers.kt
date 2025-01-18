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
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.concurrent.futures.DirectExecutor
import com.google.common.util.concurrent.ListenableFuture
import ru.solrudev.ackpine.helpers.concurrent.handleResult

@SuppressLint("RestrictedApi")
@JvmSynthetic
internal inline fun <V, R> ListenableFuture<V>.map(crossinline transform: (V) -> R): ListenableFuture<R> {
	return CallbackToFutureAdapter.getFuture { completer ->
		completer.addCancellationListener({ cancel(false) }, DirectExecutor.INSTANCE)
		handleResult(
			onException = completer::setException,
			block = { result ->
				try {
					completer.set(transform(result))
				} catch (exception: Exception) {
					completer.setException(exception)
				}
			}
		)
	}
}

@SuppressLint("RestrictedApi")
@JvmSynthetic
internal fun <V> CallbackToFutureAdapter.Completer<V>.onCancellation(block: () -> Unit) {
	addCancellationListener(block, DirectExecutor.INSTANCE)
}