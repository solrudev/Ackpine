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

package ru.solrudev.ackpine.helpers

import android.annotation.SuppressLint
import androidx.concurrent.futures.DirectExecutor
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutionException

@SuppressLint("RestrictedApi")
@JvmSynthetic
internal inline fun <V> ListenableFuture<V>.handleResult(
	crossinline onException: (Exception) -> Unit = { throw it },
	crossinline block: (V) -> Unit
) {
	if (isDone) {
		block(getAndUnwrapException())
		return
	}
	addListener({
		try {
			block(getAndUnwrapException())
		} catch (e: Exception) {
			onException(e)
		}
	}, DirectExecutor.INSTANCE)
}

private fun <V> ListenableFuture<V>.getAndUnwrapException(): V {
	val value = try {
		get()
	} catch (e: ExecutionException) {
		throw e.cause ?: e
	}
	return value
}