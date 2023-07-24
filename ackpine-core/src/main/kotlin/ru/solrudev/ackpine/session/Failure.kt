package ru.solrudev.ackpine.session

/**
 * Represents a session's failure
 */
public interface Failure {

	/**
	 * Failure occurred due to an [exception].
	 */
	public interface Exceptional {
		public val exception: Exception
	}
}