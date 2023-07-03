package ru.solrudev.ackpine.uninstaller

import ru.solrudev.ackpine.session.Failure
import java.io.Serializable

public sealed interface UninstallFailure : Failure, Serializable {

	/**
	 * The operation failed in a generic way.
	 */
	public data object Generic : UninstallFailure

	/**
	 * The operation failed because it was actively aborted.
	 * For example, the user actively declined uninstall request.
	 */
	public data class Aborted(public val message: String) : UninstallFailure

	/**
	 * The operation failed because an exception was thrown.
	 */
	public data class Exceptional(public override val exception: Exception) : UninstallFailure, Failure.Exceptional
}