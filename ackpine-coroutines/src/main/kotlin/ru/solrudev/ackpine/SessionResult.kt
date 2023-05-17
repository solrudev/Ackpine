package ru.solrudev.ackpine

public sealed interface SessionResult<T : Failure> {

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

	public data class Error<T : Failure>(public val cause: T) : SessionResult<T>
}