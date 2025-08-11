/*
 * Copyright (C) 2024 Ilya Fomichev
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

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import java.io.Closeable

/**
 * Closes [Closeable] resource and in case of failure adds the exception to suppressed exceptions of the [cause].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun Closeable.closeWithException(cause: Throwable) {
	try {
		close()
	} catch (closeException: Throwable) {
		cause.addSuppressed(closeException)
	}
}

/**
 * Closes [AutoCloseable] resource and in case of failure adds the exception to suppressed exceptions of the [cause].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(Build.VERSION_CODES.KITKAT)
public fun AutoCloseable.closeWithException(cause: Throwable) {
	try {
		close()
	} catch (closeException: Throwable) {
		cause.addSuppressed(closeException)
	}
}

/**
 * Guarantees closing all [resources] and delivery of every failure through thrown exception.
 */
@RequiresApi(Build.VERSION_CODES.KITKAT)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmSynthetic
public fun closeAll(resources: Iterable<AutoCloseable>) {
	resources
		.mapCatching { it.close() }
		.getOrThrow()
}

/**
 * Guarantees closing all [resources] and delivery of every failure through the [cause] via its suppressed exceptions.
 */
@RequiresApi(Build.VERSION_CODES.KITKAT)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmSynthetic
public fun closeAllWithException(resources: Iterable<AutoCloseable>, cause: Throwable) {
	resources
		.mapCatching { it.close() }
		.failureOrNull()
		?.let { failure -> cause.addSuppressed(failure.exception) }
}

/**
 * Guarantees closing all [resources] and delivery of every failure through thrown exception.
 */
@RequiresApi(Build.VERSION_CODES.KITKAT)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmSynthetic
public fun closeAll(vararg resources: AutoCloseable): Unit = closeAll(resources.asIterable())