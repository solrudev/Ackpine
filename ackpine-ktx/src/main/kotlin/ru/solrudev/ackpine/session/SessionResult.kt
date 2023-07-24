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