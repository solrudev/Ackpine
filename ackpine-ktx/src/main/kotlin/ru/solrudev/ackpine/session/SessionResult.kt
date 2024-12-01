/*
 * Copyright (C) 2023-2024 Ilya Fomichev
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

@file:Suppress("DEPRECATION_ERROR")

package ru.solrudev.ackpine.session

/**
 * Represents a result of a [Session].
 */
@Deprecated(
	message = "This type is redundant, Session.State.Completed covers this type's use cases. " +
			"This type will be removed in next minor release.",
	replaceWith = ReplaceWith(
		expression = "Session.State.Completed",
		imports = ["ru.solrudev.ackpine.session.Session"]
	),
	level = DeprecationLevel.ERROR
)
public sealed interface SessionResult<T : Failure> {

	/**
	 * Session completed successfully.
	 */
	@Deprecated(
		message = "This type is redundant, Session.State.Succeeded covers this type's use cases. " +
				"This type will be removed in next minor release.",
		replaceWith = ReplaceWith(
			expression = "Session.State.Succeeded",
			imports = ["ru.solrudev.ackpine.session.Session"]
		),
		level = DeprecationLevel.ERROR
	)
	public class Success<T : Failure> : SessionResult<T> {

		override fun equals(other: Any?): Boolean {
			if (this === other) {
				return true
			}
			return javaClass == other?.javaClass
		}

		override fun hashCode(): Int = javaClass.hashCode()
		override fun toString(): String = "Success"
	}

	/**
	 * Session completed with an error.
	 * @property cause an instance of [Failure] describing the error.
	 */
	@Deprecated(
		message = "This type is redundant, Session.State.Failed covers this type's use cases. " +
				"This type will be removed in next minor release.",
		replaceWith = ReplaceWith(
			expression = "Session.State.Failed",
			imports = ["ru.solrudev.ackpine.session.Session"]
		),
		level = DeprecationLevel.ERROR
	)
	public data class Error<T : Failure>(public val cause: T) : SessionResult<T>
}