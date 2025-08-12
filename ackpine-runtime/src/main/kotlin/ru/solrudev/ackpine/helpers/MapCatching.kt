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

@file:Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")

package ru.solrudev.ackpine.helpers

import androidx.annotation.RestrictTo
import ru.solrudev.ackpine.helpers.MapResult.Failure
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Returns an encapsulated list containing the results of applying the given [transform] function to each element in the
 * original collection, catching and aggregating any [Throwable] exceptions that were thrown from the [transform]
 * function execution and encapsulating them as a failure.
 *
 * This function always processes all elements and aggregates all caught exceptions. [mapCatchingFirst] processes
 * elements only until the first exception is caught.
 *
 * In case of failure, partial result of transformation is delivered to handle successfully processed elements.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmSynthetic
public inline fun <T, R> Iterable<T>.mapCatching(transform: (T) -> R): MapResult<List<R>> {
	val values = mutableListOf<R>()
	val exceptions = mutableListOf<Throwable>()
	for (element in this) {
		try {
			values += transform(element)
		} catch (throwable: Throwable) {
			exceptions += throwable
		}
	}
	val mainException = exceptions.removeFirstOrNull()
	if (mainException == null) {
		return MapResult(values)
	}
	for (exception in exceptions) {
		mainException.addSuppressed(exception)
	}
	return MapResult(Failure(partialResult = values, mainException))
}

/**
 * Returns an encapsulated list containing the results of applying the given [transform] function to each element in the
 * original collection, catching the first [Throwable] exception that was thrown from the [transform]
 * function execution, encapsulating it as a failure and terminating further operations.
 *
 * This function processes elements only until the first exception is caught. [mapCatching] always processes all
 * elements and aggregates all caught exceptions.
 *
 * In case of failure, partial result of transformation is delivered to handle successfully processed elements.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmSynthetic
public inline fun <T, R> Iterable<T>.mapCatchingFirst(transform: (T) -> R): MapResult<List<R>> {
	val values = mutableListOf<R>()
	for (element in this) {
		try {
			values += transform(element)
		} catch (exception: Throwable) {
			return MapResult(Failure(partialResult = values, exception))
		}
	}
	return MapResult(values)
}

/**
 * A discriminated union that encapsulates a successful outcome of [mapCatching] invocation with a value of type [T]
 * or a failure with an arbitrary [Throwable] exception and a partial result of type [T] of an operation.
 *
 * This class mimics Kotlin's [Result] API.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class MapResult<out T> @PublishedApi internal constructor(
	@PublishedApi
	internal val value: Any?
) {

	/**
	 * Returns `true` if this instance represents a successful outcome.
	 * In this case [isFailure] returns `false`.
	 */
	public val isSuccess: Boolean
		get() = value !is Failure<*>

	/**
	 * Returns `true` if this instance represents a failed outcome.
	 * In this case [isSuccess] returns `false`.
	 */
	public val isFailure: Boolean
		get() = value is Failure<*>

	/**
	 * Returns the encapsulated value if this instance represents [success][Result.isSuccess] or `null`
	 * if it is [failure][Result.isFailure].
	 */
	public inline fun getOrNull(): T? = when {
		isFailure -> null
		else -> value as T
	}

	/**
	 * Returns the encapsulated [Failure] if this instance represents [failure][isFailure] or `null`
	 * if it is [success][isSuccess].
	 */
	public fun failureOrNull(): Failure<T>? = when (value) {
		is Failure<*> -> value as Failure<T>
		else -> null
	}

	/**
	 * Represents an operation failure.
	 * @property partialResult a partial result of an operation. May be used to clean up resources.
	 * @property exception a [Throwable] which caused the failure.
	 */
	@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
	public class Failure<out T>(
		@JvmField
		public val partialResult: T,
		@JvmField
		public val exception: Throwable
	) {

		override fun equals(other: Any?): Boolean = other is Failure<*>
				&& partialResult == other.partialResult
				&& exception == other.exception

		override fun hashCode(): Int = 31 * partialResult.hashCode() + exception.hashCode()

		override fun toString(): String = "Failure(" +
				"partialResult=$partialResult, " +
				"exception=$exception" +
				")"
	}
}

/**
 * Returns the encapsulated value if this instance represents [success][MapResult.isSuccess] or the
 * result of [onFailure] function for the encapsulated [Failure] if it is [failure][MapResult.isFailure].
 *
 * Note, that this function rethrows any [Throwable] exception thrown by [onFailure] function.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@OptIn(ExperimentalContracts::class)
@JvmSynthetic
public inline fun <R, T : R> MapResult<T>.getOrElse(onFailure: (failure: Failure<T>) -> R): R {
	contract {
		callsInPlace(onFailure, InvocationKind.AT_MOST_ONCE)
	}
	return when (val failure = failureOrNull()) {
		null -> value as T
		else -> onFailure(failure)
	}
}

/**
 * Returns the encapsulated value if this instance represents [success][MapResult.isSuccess] or throws the encapsulated
 * [Throwable] exception if it is [failure][MapResult.isFailure].
 *
 * This function is a shorthand for `getOrElse { throw it }` (see [getOrElse]).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmSynthetic
public inline fun <T> MapResult<T>.getOrThrow(): T {
	if (value is Failure<*>) {
		throw value.exception
	}
	return value as T
}