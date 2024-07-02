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
	 */
	public data class InheritExisting(val packageName: String) : InstallMode

	private companion object {

		/**
		 * Mode for an install session whose staged APKs should fully replace any existing APKs for the target app.
		 */
		@Suppress("RedundantVisibilityModifier")
		@JvmField
		public val FULL: Full = Full
	}
}