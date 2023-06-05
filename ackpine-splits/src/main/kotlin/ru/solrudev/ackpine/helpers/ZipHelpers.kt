package ru.solrudev.ackpine.helpers

import java.util.zip.ZipInputStream

/**
 * The returned sequence is constrained to be iterated only once.
 */
@JvmSynthetic
internal fun ZipInputStream.entries() = sequence {
	while (true) {
		val entry = nextEntry ?: break
		yield(entry)
	}
}.constrainOnce()