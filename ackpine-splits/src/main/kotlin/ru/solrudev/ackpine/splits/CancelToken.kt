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

import android.os.CancellationSignal
import kotlin.coroutines.cancellation.CancellationException

/**
 * A cancellation signal for cooperative cancellation.
 */
public class CancelToken private constructor(
	@get:JvmSynthetic
	internal val signal: CancellationSignal
) {

	/**
	 * Returns `true` if this token was cancelled through its [owner][Owner].
	 */
	@Volatile
	public var isCancelled: Boolean = false
		private set

	private var cancelHandler: (() -> Unit)? = null

	/**
	 * Throws [CancellationException] if this token was cancelled.
	 */
	@JvmOverloads
	public fun throwIfCancelled(cause: Throwable? = null) {
		if (isCancelled) {
			throw CancellationException(cause)
		}
	}

	@JvmSynthetic
	internal fun onCancel(handler: () -> Unit) {
		cancelHandler = handler
	}

	private fun cancel() {
		isCancelled = true
		cancelHandler?.invoke()
		signal.cancel()
		cancelHandler = null
	}

	/**
	 * An owner of [CancelToken] which is able to cancel it.
	 */
	public class Owner {

		/**
		 * A cancel token owned by this object.
		 */
		public val token: CancelToken = CancelToken(CancellationSignal())

		/**
		 * Cancels the [token].
		 */
		public fun cancel() {
			token.cancel()
		}
	}
}