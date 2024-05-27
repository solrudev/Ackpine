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

package ru.solrudev.ackpine.installer

import android.content.pm.PackageInstaller
import ru.solrudev.ackpine.installer.InstallFailure.Aborted
import ru.solrudev.ackpine.installer.InstallFailure.Blocked
import ru.solrudev.ackpine.installer.InstallFailure.Conflict
import ru.solrudev.ackpine.installer.InstallFailure.Exceptional
import ru.solrudev.ackpine.installer.InstallFailure.Generic
import ru.solrudev.ackpine.installer.InstallFailure.Incompatible
import ru.solrudev.ackpine.installer.InstallFailure.Invalid
import ru.solrudev.ackpine.installer.InstallFailure.Storage
import ru.solrudev.ackpine.session.Failure
import java.io.Serializable

/**
 * Represents the cause of installation failure. Contains string representation in [message] property.
 *
 * May be either [Exceptional], [Generic], [Aborted], [Blocked], [Conflict], [Incompatible], [Invalid], [Storage] or
 * [Timeout].
 * @property message Detailed string representation of the status, including raw details that are useful for debugging.
 */
public sealed class InstallFailure(public open val message: String?) : Failure, Serializable {

	/**
	 * The operation failed because an exception was thrown.
	 */
	public data class Exceptional(
		public override val exception: Exception
	) : InstallFailure("Install failed due to an exception."), Failure.Exceptional {
		private companion object {
			private const val serialVersionUID: Long = -9079346194912065953L
		}
	}

	/**
	 * The operation failed in a generic way. The system will always try to provide a more specific failure reason,
	 * but in some rare cases this may be delivered.
	 */
	public data class Generic @JvmOverloads public constructor(
		public override val message: String? = null
	) : InstallFailure(message) {
		private companion object {
			private const val serialVersionUID: Long = 6785833663285844380L
		}
	}

	/**
	 * The operation failed because it was actively aborted.
	 * For example, the user actively declined requested permissions, or the session was abandoned.
	 */
	public data class Aborted(public override val message: String?) : InstallFailure(message) {
		private companion object {
			private const val serialVersionUID: Long = 5217863192717623705L
		}
	}

	/**
	 * The operation failed because it was blocked. For example, a device policy may be blocking the operation,
	 * a package verifier may have blocked the operation, or the app may be required for core system operation.
	 *
	 * The result may also contain [otherPackageName] with the specific package blocking the install.
	 */
	public data class Blocked @JvmOverloads public constructor(
		public override val message: String?,
		public val otherPackageName: String? = null
	) : InstallFailure(message) {
		private companion object {
			private const val serialVersionUID: Long = 8457207819123634411L
		}
	}

	/**
	 * The operation failed because it conflicts (or is inconsistent with) with another package already installed
	 * on the device. For example, an existing permission, incompatible certificates, etc. The user may be able to
	 * uninstall another app to fix the issue.
	 *
	 * The result may also contain [otherPackageName] with the specific package identified as the cause of the conflict.
	 */
	public data class Conflict @JvmOverloads public constructor(
		public override val message: String?,
		public val otherPackageName: String? = null
	) : InstallFailure(message) {
		private companion object {
			private const val serialVersionUID: Long = -5471020595408614795L
		}
	}

	/**
	 * The operation failed because it is fundamentally incompatible with this device. For example, the app may
	 * require a hardware feature that doesn't exist, it may be missing native code for the ABIs supported by the
	 * device, or it requires a newer SDK version, etc.
	 */
	public data class Incompatible(public override val message: String?) : InstallFailure(message) {
		private companion object {
			private const val serialVersionUID: Long = 7194911454975521530L
		}
	}

	/**
	 * The operation failed because one or more of the APKs was invalid. For example, they might be malformed,
	 * corrupt, incorrectly signed, mismatched, etc.
	 */
	public data class Invalid(public override val message: String?) : InstallFailure(message) {
		private companion object {
			private const val serialVersionUID: Long = 9045650456707265963L
		}
	}

	/**
	 * The operation failed because of storage issues. For example, the device may be running low on space,
	 * or external media may be unavailable. The user may be able to help free space or insert different external media.
	 *
	 * The result may also contain [storagePath] with the path to the storage device that caused the failure.
	 */
	public data class Storage @JvmOverloads public constructor(
		public override val message: String?,
		public val storagePath: String? = null
	) : InstallFailure(message) {
		private companion object {
			private const val serialVersionUID: Long = -7427274925677501111L
		}
	}

	/**
	 * The operation failed because it didn't complete within the specified timeout.
	 */
	public data class Timeout(public override val message: String?) : InstallFailure(message) {
		private companion object {
			private const val serialVersionUID: Long = 5534247941342428912L
		}
	}

	internal companion object {

		private const val serialVersionUID: Long = -4122677617329666142L

		/**
		 * Converts Android's [PackageInstaller] failure status code to [InstallFailure] object.
		 */
		@JvmSynthetic
		internal fun fromStatusCode(
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
			PackageInstaller.STATUS_FAILURE_TIMEOUT -> Timeout(message)
			else -> Generic()
		}
	}
}