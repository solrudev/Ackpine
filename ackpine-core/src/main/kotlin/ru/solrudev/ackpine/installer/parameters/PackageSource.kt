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

import ru.solrudev.ackpine.installer.parameters.PackageSource.DownloadedFile
import ru.solrudev.ackpine.installer.parameters.PackageSource.LocalFile
import ru.solrudev.ackpine.installer.parameters.PackageSource.Other
import ru.solrudev.ackpine.installer.parameters.PackageSource.Store
import ru.solrudev.ackpine.installer.parameters.PackageSource.Unspecified

/**
 * Indicates the package source of the app being installed. This is informational and may be used as a signal by the
 * system.
 *
 * Default value is [Unspecified].
 */
public sealed class PackageSource(
	@get:JvmSynthetic
	internal val ordinal: Int
) {

	/**
	 * The installer did not specify the package source. Default value.
	 */
	public data object Unspecified : PackageSource(0)

	/**
	 * The package being installed is from a store. An app store that installs an app for the user would use this.
	 */
	public data object Store : PackageSource(1)

	/**
	 * The package being installed comes from a local file on the device. A file manager that is facilitating the
	 * installation of an APK file would use this.
	 */
	public data object LocalFile : PackageSource(2)

	/**
	 * The package being installed comes from a file that was downloaded to the device by the user. For use in place
	 * of [LocalFile] when the installer knows the package was downloaded.
	 */
	public data object DownloadedFile : PackageSource(3)

	/**
	 * The package being installed is from a source not reflected by any other package source constant.
	 */
	public data object Other : PackageSource(4)

	@Suppress("Unused")
	private data object NonExhaustiveWhenGuard : PackageSource(-1)

	@Suppress("RedundantVisibilityModifier")
	private companion object {

		/**
		 * The installer did not specify the package source. Default value.
		 */
		@JvmField
		public val UNSPECIFIED: PackageSource = Unspecified

		/**
		 * The package being installed is from a store. An app store that installs an app for the user would use this.
		 */
		@JvmField
		public val STORE: PackageSource = Store

		/**
		 * The package being installed comes from a local file on the device. A file manager that is facilitating the
		 * installation of an APK file would use this.
		 */
		@JvmField
		public val LOCAL_FILE: PackageSource = LocalFile

		/**
		 * The package being installed comes from a file that was downloaded to the device by the user. For use in place
		 * of [LOCAL_FILE] when the installer knows the package was downloaded.
		 */
		@JvmField
		public val DOWNLOADED_FILE: PackageSource = DownloadedFile

		/**
		 * The package being installed is from a source not reflected by any other package source constant.
		 */
		@JvmField
		public val OTHER: PackageSource = Other
	}
}

@get:JvmSynthetic
internal val packageSources = arrayOf(Unspecified, Store, LocalFile, DownloadedFile, Other)