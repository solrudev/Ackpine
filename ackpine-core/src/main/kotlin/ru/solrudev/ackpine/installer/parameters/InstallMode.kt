/*
 * Copyright (C) 2024 Ilya Fomichev
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

package ru.solrudev.ackpine.installer.parameters

import ru.solrudev.ackpine.installer.parameters.InstallMode.Full
import ru.solrudev.ackpine.installer.parameters.InstallMode.InheritExisting
import ru.solrudev.ackpine.installer.parameters.InstallerType.INTENT_BASED

/**
 * Mode for an install session. Takes effect only when using [InstallerType.SESSION_BASED] installer.
 *
 * * [Full] (default) &mdash; mode for an install session whose staged APKs should fully replace any existing APKs for
 *   the target app.
 * * [InheritExisting] &mdash; mode for an install session that should inherit any existing APKs for the target app,
 *   unless they have been explicitly overridden (based on split name) by the session.
 */
public sealed interface InstallMode {

	/**
	 * Mode for an install session whose staged APKs should fully replace any existing APKs for the target app.
	 */
	public data object Full : InstallMode

	/**
	 * Mode for an install session that should inherit any existing APKs for the target app, unless they have been
	 * explicitly overridden (based on split name) by the session. For example, this can be used to add one or more
	 * split APKs to an existing installation.
	 *
	 * If there are no existing APKs for the target app, this behaves like [Full].
	 *
	 * When using [INTENT_BASED] installer, this mode is ignored.
	 *
	 * @property packageName Package name of the app being installed. If the APKs staged in the session aren't
	 * consistent with this package name, the install will fail.
	 *
	 * @property dontKillApp Requests that the system not kill any of the package's running processes as part of a
	 * session in which splits being added. By default, all installs will result in the package's running processes
	 * being killed before the install completes.
	 *
	 * Takes effect only on API level >= 34.
	 */
	public data class InheritExisting @JvmOverloads public constructor(
		val packageName: String,
		val dontKillApp: Boolean = false
	) : InstallMode {

		@Deprecated(message = "Binary compatibility", level = DeprecationLevel.HIDDEN)
		public fun copy(packageName: String = this.packageName): InheritExisting {
			return InheritExisting(packageName, dontKillApp)
		}
	}

	private companion object {

		/**
		 * Mode for an install session whose staged APKs should fully replace any existing APKs for the target app.
		 */
		@Suppress("RedundantVisibilityModifier")
		@JvmField
		public val FULL: Full = Full
	}
}