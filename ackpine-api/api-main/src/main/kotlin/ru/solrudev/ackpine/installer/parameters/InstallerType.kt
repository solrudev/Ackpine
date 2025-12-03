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

package ru.solrudev.ackpine.installer.parameters

import android.Manifest.permission.REQUEST_INSTALL_PACKAGES
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Intent.ACTION_INSTALL_PACKAGE
import android.content.pm.PackageInstaller
import android.os.Build
import androidx.annotation.RequiresApi
import ru.solrudev.ackpine.installer.parameters.InstallerType.INTENT_BASED
import ru.solrudev.ackpine.installer.parameters.InstallerType.SESSION_BASED
import ru.solrudev.ackpine.isPackageInstallerApiAvailable

/**
 * Type of the package installer implementation.
 *
 * * [INTENT_BASED] &mdash; package installer will use the [ACTION_INSTALL_PACKAGE] intent action to install the
 * 	 package.
 * * [SESSION_BASED] &mdash; package installer will use system's [PackageInstaller] API to install the package.
 */
public enum class InstallerType {

	/**
	 * Package installer will use the [ACTION_INSTALL_PACKAGE] intent action to install the package.
	 *
	 * Requires [WRITE_EXTERNAL_STORAGE] and [REQUEST_INSTALL_PACKAGES] permissions.
	 */
	INTENT_BASED,

	/**
	 * Package installer will use system's [PackageInstaller] API to install the package.
	 *
	 * Requires [REQUEST_INSTALL_PACKAGES] permission.
	 */
	@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
	SESSION_BASED;

	public companion object {

		/**
		 * Default type of the package installer implementation.
		 *
		 * On API level < 21, the default value is [InstallerType.INTENT_BASED].
		 *
		 * On API level >= 21, the default value is [InstallerType.SESSION_BASED].
		 */
		@JvmField
		public val DEFAULT: InstallerType = if (isPackageInstallerApiAvailable()) SESSION_BASED else INTENT_BASED
	}
}