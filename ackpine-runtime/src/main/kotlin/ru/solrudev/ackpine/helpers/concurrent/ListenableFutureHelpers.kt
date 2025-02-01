/*
 * Copyright (C) 2023-2025 Ilya Fomichev
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

import android.annotation.SuppressLint
import androidx.concurrent.futures.DirectExecutor
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutionException

@SuppressLint("RestrictedApi")
public fun <V> ListenableFuture<V>.handleResult(block: (V) -> Unit) {
	if (isDone) {
		getAndUnwrapException()
			.onSuccess(block)
			.onFailure { throw it }
		return
	}
	addListener({
		getAndUnwrapException()
			.onSuccess(block)
			.onFailure { throw it }
	}, DirectExecutor.INSTANCE)
}

@SuppressLint("RestrictedApi")
public fun <V> ListenableFuture<V>.handleResult(
	onException: (Exception) -> Unit,
	block: (V) -> Unit
) {
	if (isDone) {
		getAndUnwrapException()
			.onSuccess(block)
			.onFailure { throwable -> if (throwable is Exception) onException(throwable) else throw throwable }
		return
	}
	addListener({
		getAndUnwrapException()
			.onSuccess(block)
			.onFailure { throwable -> if (throwable is Exception) onException(throwable) else throw throwable }
	}, DirectExecutor.INSTANCE)
}

private fun <V> ListenableFuture<V>.getAndUnwrapException(): Result<V> {
	return try {
		Result.success(get())
	} catch (exception: ExecutionException) {
		Result.failure(exception.cause ?: exception)
	}
}