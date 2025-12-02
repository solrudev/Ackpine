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

@file:Suppress("Unused", "ConstPropertyName")

package ru.solrudev.ackpine.uninstaller

import ru.solrudev.ackpine.session.Failure
import java.io.Serializable

/**
 * Represents the cause of uninstallation failure.
 */
public sealed class UninstallFailure(public open val message: String?) : Failure, Serializable {

	/**
	 * The operation failed in a generic way.
	 */
	public data class Generic @JvmOverloads public constructor(
		public override val message: String? = null
	) : UninstallFailure(message) {

		private companion object {

			@Deprecated(message = "Binary and Java source compatibility", level = DeprecationLevel.HIDDEN)
			@Suppress("RedundantVisibilityModifier")
			@JvmField
			public val INSTANCE: Generic = Generic()

			private const val serialVersionUID: Long = 90119473338043981L
		}
	}

	/**
	 * The operation failed because it was actively aborted.
	 * For example, the user actively declined uninstall request.
	 */
	public data class Aborted(public override val message: String?) : UninstallFailure(message) {
		private companion object {
			private const val serialVersionUID: Long = -1370498116182324292L
		}
	}

	/**
	 * The operation failed because it was blocked. For example, a device policy may be blocking the operation,
	 * a package verifier may have blocked the operation, or the app may be required for core system operation.
	 *
	 * The result may also contain [otherPackageName] with the specific package blocking the uninstall.
	 */
	public data class Blocked @JvmOverloads public constructor(
		public override val message: String?,
		public val otherPackageName: String? = null
	) : UninstallFailure(message) {
		private companion object {
			private const val serialVersionUID: Long = 7333688133898205490L
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
	) : UninstallFailure(message) {
		private companion object {
			private const val serialVersionUID: Long = 2582436141136856908L
		}
	}

	/**
	 * The operation failed because an exception was thrown.
	 */
	public data class Exceptional(
		public override val exception: Exception
	) : UninstallFailure("Uninstall failed due to an exception."), Failure.Exceptional {
		private companion object {
			private const val serialVersionUID = -3918656046001035393L
		}
	}

	@Suppress("Unused")
	private data object NonExhaustiveWhenGuard : UninstallFailure(message = null) {
		private const val serialVersionUID = 6803470565073569530L
		private fun readResolve(): Any = NonExhaustiveWhenGuard
	}
}