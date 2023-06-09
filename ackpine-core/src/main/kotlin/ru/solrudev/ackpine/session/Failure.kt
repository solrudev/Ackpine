package ru.solrudev.ackpine.session

public interface Failure {

	public interface Exceptional {
		public val exception: Exception
	}
}