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

package ru.solrudev.ackpine.uninstaller.parameters

import android.Manifest.permission.REQUEST_DELETE_PACKAGES
import android.content.Intent.ACTION_DELETE
import android.content.Intent.ACTION_UNINSTALL_PACKAGE
import android.content.pm.PackageInstaller
import android.os.Build
import androidx.annotation.RequiresApi
import ru.solrudev.ackpine.installer.parameters.areSplitPackagesSupported
import ru.solrudev.ackpine.uninstaller.parameters.UninstallerType.INTENT_BASED
import ru.solrudev.ackpine.uninstaller.parameters.UninstallerType.PACKAGE_INSTALLER_BASED

/**
 * Type of the package uninstaller implementation.
 *
 * * [INTENT_BASED] &mdash; package uninstaller will use the [ACTION_UNINSTALL_PACKAGE] or [ACTION_DELETE] intent action
 *   to uninstall the package.
 * * [PACKAGE_INSTALLER_BASED] &mdash; package installer will use system's [PackageInstaller] API to uninstall the
 *   package.
 */
public enum class UninstallerType {

	/**
	 * Package uninstaller will use the [ACTION_UNINSTALL_PACKAGE] or [ACTION_DELETE] intent action to uninstall the
	 * package.
	 *
	 * Requires [REQUEST_DELETE_PACKAGES] permission.
	 */
	INTENT_BASED,

	/**
	 * Package uninstaller will use system's [PackageInstaller] API to uninstall the package.
	 *
	 * Requires [REQUEST_DELETE_PACKAGES] permission.
	 */
	@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
	PACKAGE_INSTALLER_BASED;

	public companion object {

		/**
		 * Default type of the package uninstaller implementation.
		 *
		 * On API level < 21, the default value is [INTENT_BASED].
		 *
		 * On API level >= 21, the default value is [PACKAGE_INSTALLER_BASED].
		 */
		@JvmField
		public val DEFAULT: UninstallerType = if (areSplitPackagesSupported()) PACKAGE_INSTALLER_BASED else INTENT_BASED
	}
}