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
import kotlin.coroutines.RestrictsSuspension

/**
 * A [Sequence] which has [AutoCloseable] resources. Constrained to be iterated only once.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal interface CloseableSequence<T> : Sequence<T>, AutoCloseable

/**
 * Builds a [CloseableSequence] lazily yielding values one by one.
 */
@Suppress("ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL")
@JvmSynthetic
internal fun <T> closeableSequence(block: suspend CloseableSequenceScope<T>.() -> Unit): CloseableSequence<T> {
	var scope: SequenceScope<T>? = null
	var iterator: Iterator<T>? = null
	val sequence = CloseableSequenceImpl(
		iteratorProducer = { iterator!! }, scopeProducer = { scope!! }
	)
	iterator = iterator {
		scope = this
		sequence.block()
	}
	return sequence
}

/**
 * The scope for yielding values of a [CloseableSequence], provides [yield] and [yieldAll] suspension functions and
 * [addCloseableResource] function to manage [AutoCloseable] resources.
 *
 * @see closeableSequence
 */
@Suppress("ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL")
@RestrictTo(RestrictTo.Scope.LIBRARY)
@RestrictsSuspension
internal abstract class CloseableSequenceScope<T>(scopeProducer: () -> SequenceScope<T>) {

	private val scope by lazy(LazyThreadSafetyMode.PUBLICATION, scopeProducer)

	/**
	 * Adds a [resource] to a set of [AutoCloseable] resources managed by the [CloseableSequence].
	 */
	abstract fun addCloseableResource(resource: AutoCloseable)

	/**
	 * Yields a value to the [Iterator] being built and suspends until the next value is requested.
	 */
	suspend fun yield(value: T) = scope.yield(value)

	/**
	 * Yields potentially infinite sequence of values  to the [Iterator] being built and suspends until all these values
	 * are iterated and the next one is requested.
	 *
	 * The sequence can be potentially infinite.
	 */
	suspend fun yieldAll(sequence: Sequence<T>) = scope.yieldAll(sequence)
}

private class CloseableSequenceImpl<T>(
	iteratorProducer: () -> Iterator<T>,
	scopeProducer: () -> SequenceScope<T>
) : CloseableSequenceScope<T>(scopeProducer), CloseableSequence<T> {

	private val iterator by lazy(LazyThreadSafetyMode.PUBLICATION, iteratorProducer)
	private val resources = mutableSetOf<AutoCloseable>()

	@Volatile
	private var isConsumed = false

	@Volatile
	private var isClosed = false

	override fun iterator(): Iterator<T> {
		if (isConsumed) {
			throw IllegalStateException("This sequence can be consumed only once.")
		}
		isConsumed = true
		return object : Iterator<T> {
			override fun hasNext() = iterator.hasNext() && !isClosed
			override fun next() = iterator.next()
		}
	}

	override fun close() {
		isClosed = true
		for (resource in resources) {
			resource.close()
		}
	}

	override fun addCloseableResource(resource: AutoCloseable) {
		resources += resource
	}
}