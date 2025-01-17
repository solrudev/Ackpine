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

package ru.solrudev.ackpine.splits

import androidx.annotation.RestrictTo
import ru.solrudev.ackpine.helpers.closeAll
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.RestrictsSuspension

/**
 * A [Sequence] which has [AutoCloseable] resources. Constrained to be iterated only once.
 *
 * When iteration is completed or interrupted with exception originating from this sequence, all resources are
 * automatically closed.
 */
public interface CloseableSequence<out T> : Sequence<T>, AutoCloseable {

	/**
	 * Returns whether the [CloseableSequence] was closed though a call to [close].
	 */
	public val isClosed: Boolean
}

/**
 * Builds a [CloseableSequence] lazily yielding values one by one.
 */
@JvmSynthetic
internal fun <T> closeableSequence(block: suspend CloseableSequenceScope<T>.() -> Unit): CloseableSequence<T> {
	return CloseableSequenceImpl(block)
}

/**
 * The scope for yielding values of a [CloseableSequence], provides [yield] and [yieldAll] suspension functions and
 * [use] function to manage [AutoCloseable] resources.
 *
 * @see closeableSequence
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@RestrictsSuspension
internal interface CloseableSequenceScope<T> {

	/**
	 * Returns whether the [CloseableSequence] was closed though a call to [close][CloseableSequence.close].
	 */
	val isClosed: Boolean

	/**
	 * Adds a resource to a set of [AutoCloseable] resources managed by the [CloseableSequence].
	 */
	fun <T : AutoCloseable> T.use(): T

	/**
	 * Yields a value to the [Iterator] being built and suspends until the next value is requested.
	 */
	suspend fun yield(value: T)

	/**
	 * Yields potentially infinite sequence of values  to the [Iterator] being built and suspends until all these values
	 * are iterated and the next one is requested.
	 *
	 * The sequence can be potentially infinite.
	 */
	suspend fun yieldAll(sequence: Sequence<T>)
}

@Suppress("ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL")
private class CloseableSequenceImpl<T>(
	private val block: suspend CloseableSequenceScope<T>.() -> Unit
) : CloseableSequence<T>, CloseableSequenceScope<T> {

	@Volatile
	override var isClosed = false

	private val isConsumed = AtomicBoolean(false)

	@Volatile
	private lateinit var scope: SequenceScope<T>

	private val resources = Collections.newSetFromMap(
		ConcurrentHashMap<AutoCloseable, Boolean>()
	)

	override fun iterator(): Iterator<T> {
		check(isConsumed.compareAndSet(false, true)) {
			"This sequence can be consumed only once."
		}
		return iterator {
			scope = this
			use {
				block()
			}
		}
	}

	override fun close() {
		isClosed = true
		try {
			closeAll(resources)
		} finally {
			resources.clear()
		}
	}

	override fun <T : AutoCloseable> T.use(): T {
		resources += this
		return this
	}

	override suspend fun yield(value: T) = scope.yield(value)
	override suspend fun yieldAll(sequence: Sequence<T>) = scope.yieldAll(sequence)
}