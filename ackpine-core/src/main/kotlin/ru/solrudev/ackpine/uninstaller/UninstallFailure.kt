package ru.solrudev.ackpine.uninstaller

import ru.solrudev.ackpine.session.Failure
import java.io.Serializable

public sealed interface UninstallFailure : Failure, Serializable {

	/**
	 * The operation failed in a generic way.
	 */
	public data object Generic : UninstallFailure

	/**
	 * The operation failed because an exception was thrown.
	 */
	public data class Exceptional(public override val exception: Exception) : UninstallFailure, Failure.Exceptional
}