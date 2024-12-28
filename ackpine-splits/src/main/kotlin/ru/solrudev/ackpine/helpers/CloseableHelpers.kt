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

import java.io.Closeable

/**
 * Closes [Closeable] resource and adds it to suppressed exceptions of the [cause] if closing failed.
 */
@JvmSynthetic
internal fun Closeable.closeWithException(cause: Throwable) {
	try {
		close()
	} catch (closeException: Throwable) {
		cause.addSuppressed(closeException)
	}
}

/**
 * Guarantees closing all [resources] and delivery of every failure through thrown exception.
 */
@JvmSynthetic
internal fun closeAll(vararg resources: AutoCloseable) {
	val exceptions = resources.mapNotNullTo(mutableListOf()) { resource ->
		runCatching { resource.close() }.exceptionOrNull()
	}
	val closeException = exceptions.firstOrNull()
	exceptions.removeFirstOrNull()
	if (closeException != null) {
		exceptions.forEach(closeException::addSuppressed)
		throw closeException
	}
}