package ru.solrudev.ackpine

import android.content.pm.PackageInstaller
import ru.solrudev.ackpine.InstallFailure.Aborted
import ru.solrudev.ackpine.InstallFailure.Blocked
import ru.solrudev.ackpine.InstallFailure.Conflict
import ru.solrudev.ackpine.InstallFailure.Exceptional
import ru.solrudev.ackpine.InstallFailure.Generic
import ru.solrudev.ackpine.InstallFailure.Incompatible
import ru.solrudev.ackpine.InstallFailure.Invalid
import ru.solrudev.ackpine.InstallFailure.Storage

public sealed interface Failure {

	public sealed interface Exceptional {
		public val exception: Exception
	}
}

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

/**
 * Represents the cause of installation failure. Contains string representation in [message] property.
 *
 * May be either [Exceptional], [Generic], [Aborted], [Blocked], [Conflict], [Incompatible], [Invalid] or [Storage].
 * @property message Detailed string representation of the status, including raw details that are useful for debugging.
 */
public sealed class InstallFailure(public open val message: String?) : Failure {

	/**
	 * The operation failed because an exception was thrown.
	 */
	public data class Exceptional(
		public override val exception: Exception
	) : InstallFailure("Install failed due to an exception."), Failure.Exceptional

	/**
	 * The operation failed in a generic way. The system will always try to provide a more specific failure reason,
	 * but in some rare cases this may be delivered.
	 */
	public data class Generic(public override val message: String? = null) : InstallFailure(message)

	/**
	 * The operation failed because it was actively aborted.
	 * For example, the user actively declined requested permissions, or the session was abandoned.
	 */
	public data class Aborted(public override val message: String?) : InstallFailure(message)

	/**
	 * The operation failed because it was blocked. For example, a device policy may be blocking the operation,
	 * a package verifier may have blocked the operation, or the app may be required for core system operation.
	 *
	 * The result may also contain [otherPackageName] with the specific package blocking the install.
	 */
	public data class Blocked(
		public override val message: String?,
		public val otherPackageName: String? = null
	) : InstallFailure(message)

	/**
	 * The operation failed because it conflicts (or is inconsistent with) with another package already installed
	 * on the device. For example, an existing permission, incompatible certificates, etc. The user may be able to
	 * uninstall another app to fix the issue.
	 *
	 * The result may also contain [otherPackageName] with the specific package identified as the cause of the conflict.
	 */
	public data class Conflict(
		public override val message: String?,
		public val otherPackageName: String? = null
	) : InstallFailure(message)

	/**
	 * The operation failed because it is fundamentally incompatible with this device. For example, the app may
	 * require a hardware feature that doesn't exist, it may be missing native code for the ABIs supported by the
	 * device, or it requires a newer SDK version, etc.
	 */
	public data class Incompatible(public override val message: String?) : InstallFailure(message)

	/**
	 * The operation failed because one or more of the APKs was invalid. For example, they might be malformed,
	 * corrupt, incorrectly signed, mismatched, etc.
	 */
	public data class Invalid(public override val message: String?) : InstallFailure(message)

	/**
	 * The operation failed because of storage issues. For example, the device may be running low on space,
	 * or external media may be unavailable. The user may be able to help free space or insert different external media.
	 *
	 * The result may also contain [storagePath] with the path to the storage device that caused the failure.
	 */
	public data class Storage(
		public override val message: String?,
		public val storagePath: String? = null
	) : InstallFailure(message)

	public companion object {

		/**
		 * Converts Android's [PackageInstaller] failure status code to [InstallFailure] object.
		 */
		@JvmStatic
		public fun fromStatusCode(
			statusCode: Int,
			message: String? = null,
			otherPackageName: String? = null,
			storagePath: String? = null
		): InstallFailure = when (statusCode) {
			PackageInstaller.STATUS_FAILURE -> Generic(message)
			PackageInstaller.STATUS_FAILURE_ABORTED -> Aborted(message)
			PackageInstaller.STATUS_FAILURE_BLOCKED -> Blocked(message, otherPackageName)
			PackageInstaller.STATUS_FAILURE_CONFLICT -> Conflict(message, otherPackageName)
			PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> Incompatible(message)
			PackageInstaller.STATUS_FAILURE_INVALID -> Invalid(message)
			PackageInstaller.STATUS_FAILURE_STORAGE -> Storage(message, storagePath)
			else -> Generic()
		}
	}
}