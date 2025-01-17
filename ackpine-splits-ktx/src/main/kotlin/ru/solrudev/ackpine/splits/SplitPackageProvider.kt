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

package ru.solrudev.ackpine.splits

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * A suspending variant of [get][SplitPackage.Provider.get].
 *
 * [get][SplitPackage.Provider.get] will be called in the specified [context], by default [Dispatchers.IO].
 *
 * This function should **not** be called with main thread context, because underlying [get][SplitPackage.Provider.get]
 * can be blocking.
 *
 * This suspending function is cancellable.
 * If the [Job] of the current coroutine is cancelled or completed while this suspending function is waiting, this
 * function immediately resumes with [CancellationException].
 */
public suspend fun SplitPackage.Provider.get(context: CoroutineContext = Dispatchers.IO): SplitPackage {
	return executeCancellable(context, ::get)
}

/**
 * A suspending variant of [toList][SplitPackage.Provider.toList].
 *
 * [toList][SplitPackage.Provider.toList] will be called in the specified [context], by default [Dispatchers.IO].
 *
 * This function should **not** be called with main thread context, because underlying
 * [toList][SplitPackage.Provider.toList] can be blocking.
 *
 * This suspending function is cancellable.
 * If the [Job] of the current coroutine is cancelled or completed while this suspending function is waiting, this
 * function immediately resumes with [CancellationException].
 */
public suspend fun SplitPackage.Provider.toList(context: CoroutineContext = Dispatchers.IO): List<Apk> {
	return executeCancellable(context, ::toList)
}

private suspend fun <R> executeCancellable(
	context: CoroutineContext,
	action: (CancelToken) -> R
) = withContext(context) {
	suspendCancellableCoroutine { continuation ->
		val cancelTokenOwner = CancelToken.Owner()
		continuation.invokeOnCancellation {
			cancelTokenOwner.cancel()
		}
		try {
			continuation.resume(action(cancelTokenOwner.token))
		} catch (exception: Throwable) {
			continuation.resumeWithException(exception)
		}
	}
}