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

package ru.solrudev.ackpine.splits.helpers

import androidx.annotation.RestrictTo
import java.io.InputStream

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class NonClosingInputStream private constructor(private val inputStream: InputStream) : InputStream() {
	override fun read(): Int = inputStream.read()
	override fun available(): Int = inputStream.available()
	override fun markSupported(): Boolean = inputStream.markSupported()
	override fun mark(readlimit: Int) = inputStream.mark(readlimit)
	override fun read(b: ByteArray?): Int = inputStream.read(b)
	override fun read(b: ByteArray?, off: Int, len: Int): Int = inputStream.read(b, off, len)
	override fun reset() = inputStream.reset()
	override fun skip(n: Long): Long = inputStream.skip(n)

	override fun close() {
		// no-op
	}

	override fun equals(other: Any?): Boolean {
		if (other !is NonClosingInputStream) {
			return false
		}
		return inputStream == other
	}

	override fun hashCode(): Int = inputStream.hashCode()
	override fun toString(): String = inputStream.toString()

	internal companion object {

		@JvmSynthetic
		internal fun InputStream.nonClosing() = NonClosingInputStream(this)
	}
}