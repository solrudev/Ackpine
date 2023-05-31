package ru.solrudev.ackpine.helpers

import java.io.InputStream

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

	override fun hashCode(): Int = inputStream.hashCode()
	override fun equals(other: Any?): Boolean = inputStream == other
	override fun toString(): String = inputStream.toString()

	internal companion object {

		@JvmSynthetic
		internal fun InputStream.nonClosing() = NonClosingInputStream(this)
	}
}