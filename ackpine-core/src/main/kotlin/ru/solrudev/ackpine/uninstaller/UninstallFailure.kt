package ru.solrudev.ackpine.uninstaller

import ru.solrudev.ackpine.session.Failure

public sealed interface UninstallFailure : Failure {

	/**
	 * The operation failed in a generic way.
	 */
	public data object Generic : UninstallFailure

	/**
	 * The operation failed because an exception was thrown.
	 */
	public data class Exceptional(public override val exception: Exception) : UninstallFailure, Failure.Exceptional
}