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

package ru.solrudev.ackpine.session

/**
 * Represents a result of a [Session].
 */
public sealed interface SessionResult<T : Failure> {

	/**
	 * Session completed successfully.
	 */
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
	public data class Error<T : Failure>(public val cause: T) : SessionResult<T>
}