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

package ru.solrudev.ackpine.io

import ru.solrudev.ackpine.compress.utils.BoundedInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.ByteBuffer

/**
 * Buffers a stream with the expected [size], or `-1` if unknown, up to [maxSize] bytes.
 * Returns `null` if the size is invalid, exceeds the limit, or differs from the actual number of bytes.
 */
@JvmSynthetic
internal fun InputStream.toByteBuffer(size: Long, maxSize: Int): ByteBuffer? {
	require(maxSize > 0) { "maxSize should be larger than 0, but was $maxSize" }
	if (size < -1 || size > maxSize) {
		return null
	}
	val initialCapacity = if (size >= 0) size.toInt() else minOf(DEFAULT_BUFFER_SIZE, maxSize)
	val buffer = object : ByteArrayOutputStream(initialCapacity) {
		fun toByteBuffer() = ByteBuffer.wrap(buf, 0, count)
	}
	val bytesRead = BoundedInputStream.builder()
		.setInputStream(this)
		.setMaxCount(if (size >= 0) size else maxSize.toLong())
		.get()
		.copyTo(buffer)
	if (size >= 0 && bytesRead != size) {
		return null
	}
	return if (read() == -1) buffer.toByteBuffer() else null
}