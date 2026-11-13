/*
 * Copyright (C) 2026 Ilya Fomichev
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

import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

class ToByteBufferTest {

	@Test
	fun readsKnownSizeIntoExactSizedBuffer() {
		val bytes = byteArrayOf(1, 2, 3)
		val buffer = assertNotNull(bytes.inputStream().toByteBuffer(bytes.size.toLong(), 32))
		assertContentEquals(bytes, buffer.bytes())
		assertEquals(bytes.size, buffer.capacity())
	}

	@Test
	fun acceptsEmptyStreamsWithKnownAndUnknownSizes() {
		for (size in listOf(0L, -1L)) {
			val buffer = assertNotNull(byteArrayOf().inputStream().toByteBuffer(size, 32))
			assertEquals(0, buffer.remaining())
		}
	}

	@Test
	fun rejectsInvalidDeclaredSizesBeforeReading() {
		val stream = object : InputStream() {
			override fun read() = error("The stream must not be read")
		}
		for (size in listOf(-2L, 33L, Int.MAX_VALUE.toLong() + 1, Long.MAX_VALUE)) {
			assertNull(stream.toByteBuffer(size, 32))
		}
	}

	@Test
	fun rejectsTruncatedKnownSize() {
		assertNull(byteArrayOf(1, 2).inputStream().toByteBuffer(3, 32))
	}

	@Test
	fun rejectsBytesBeyondKnownSize() {
		assertNull(byteArrayOf(1, 2, 3).inputStream().toByteBuffer(2, 32))
	}

	@Test
	fun readsUnknownSizeAcrossBufferGrowth() {
		val bytes = ByteArray(10_000) { it.toByte() }
		val buffer = assertNotNull(bytes.inputStream().toByteBuffer(-1, 10_001))
		assertContentEquals(bytes, buffer.bytes())
	}

	@Test
	fun acceptsStreamsExactlyAtTheLimit() {
		val bytes = ByteArray(10_000) { it.toByte() }
		for (size in listOf(bytes.size.toLong(), -1L)) {
			val buffer = assertNotNull(bytes.inputStream().toByteBuffer(size, bytes.size))
			assertContentEquals(bytes, buffer.bytes())
		}
	}

	@Test
	fun stopsAnEndlessStreamAfterTheLimitAndOneProbeByte() {
		val stream = object : InputStream() {
			var bytesRead = 0

			override fun read(): Int {
				bytesRead++
				check(bytesRead <= 10_001)
				return 1
			}
		}
		assertNull(stream.toByteBuffer(-1, 10_000))
		assertEquals(10_001, stream.bytesRead)
	}

	@Test
	fun handlesShortReads() {
		val bytes = byteArrayOf(1, 2, 3, 4, 5)
		for (size in listOf(bytes.size.toLong(), -1L)) {
			val stream = object : FilterInputStream(bytes.inputStream()) {
				override fun read(buffer: ByteArray, off: Int, len: Int) = super.read(buffer, off, minOf(2, len))
			}
			val buffer = assertNotNull(stream.toByteBuffer(size, 32))
			assertContentEquals(bytes, buffer.bytes())
		}
	}

	@Test
	fun propagatesIoFailures() {
		val failure = IOException("Read failed")
		val stream = object : InputStream() {
			override fun read() = throw failure
		}
		assertSame(failure, assertFailsWith<IOException> { stream.toByteBuffer(1, 32) })
	}

	private fun ByteBuffer.bytes() = ByteArray(remaining()).also { get(it) }
}