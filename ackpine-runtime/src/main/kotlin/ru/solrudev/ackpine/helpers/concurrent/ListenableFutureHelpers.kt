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

@file:Suppress("UPPER_BOUND_VIOLATED_BASED_ON_JAVA_ANNOTATIONS")

package ru.solrudev.ackpine.helpers.concurrent

import android.annotation.SuppressLint
import androidx.annotation.RestrictTo
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.concurrent.futures.DirectExecutor
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor

/**
 * Attaches a listener which will execute [block] on the [executor] when the future completes.
 *
 * In case when the future is completed with an exception, the exception will be thrown.
 *
 * @param executor an [Executor] on which the [block] will be executed.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@SuppressLint("RestrictedApi")
@JvmOverloads
public fun <V> ListenableFuture<V>.handleResult(
	executor: Executor = DirectExecutor.INSTANCE,
	block: (V) -> Unit
) {
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
	}, executor)
}

/**
 * Attaches a listener which will execute [block] on the [executor] when the future completes.
 *
 * In case when the future is completed with an exception, the exception will be delivered via [onException] callback.
 *
 * @param executor an [Executor] on which the [block] will be executed.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@SuppressLint("RestrictedApi")
@JvmOverloads
public fun <V> ListenableFuture<V>.handleResult(
	executor: Executor = DirectExecutor.INSTANCE,
	onException: (Exception) -> Unit,
	block: (V) -> Unit
) {
	if (isDone) {
		getAndUnwrapException()
			.onSuccess(block)
			.onFailure { throwable ->
				if (throwable is Exception) {
					onException(throwable)
				} else {
					onException(RuntimeException(throwable))
				}
			}
		return
	}
	addListener({
		getAndUnwrapException()
			.onSuccess(block)
			.onFailure { throwable ->
				if (throwable is Exception) {
					onException(throwable)
				} else {
					onException(RuntimeException(throwable))
				}
			}
	}, executor)
}

/**
 * Returns a new [ListenableFuture] which will complete with a result of applying the given [transform] function to the
 * value got from the original future.
 *
 * Any exception thrown from [transform] will be caught and delivered through this future.
 *
 * @param executor executor an [Executor] on which the [transform] will be executed.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@SuppressLint("RestrictedApi")
public fun <V, R> ListenableFuture<V>.map(
	executor: Executor,
	transform: (V) -> R
): ListenableFuture<R> {
	return CallbackToFutureAdapter.getFuture { completer ->
		completer.addCancellationListener({ cancel(false) }, DirectExecutor.INSTANCE)
		handleResult(
			executor,
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

/**
 * Returns a new [ListenableFuture] which will complete with a result of applying the given [transform] function to the
 * value got from the original future.
 *
 * Any exception thrown from [transform] will be caught and delivered through this future.
 *
 * [transform] will be executed on whatever thread that completes the original future.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@SuppressLint("RestrictedApi")
public fun <V, R> ListenableFuture<V>.map(transform: (V) -> R): ListenableFuture<R> {
	return map(DirectExecutor.INSTANCE, transform)
}

private fun <V> ListenableFuture<V>.getAndUnwrapException(): Result<V> {
	return try {
		Result.success(get())
	} catch (exception: ExecutionException) {
		Result.failure(exception.cause ?: exception)
	} catch (exception: Exception) {
		Result.failure(exception)
	}
}