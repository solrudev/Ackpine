package ru.solrudev.ackpine.installer.parameters

import android.content.Intent.ACTION_INSTALL_PACKAGE
import android.content.pm.PackageInstaller
import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.RequiresApi
import ru.solrudev.ackpine.installer.parameters.InstallerType.INTENT_BASED
import ru.solrudev.ackpine.installer.parameters.InstallerType.SESSION_BASED
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.REQUEST_INSTALL_PACKAGES

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
	 * Requires [READ_EXTERNAL_STORAGE] and [REQUEST_INSTALL_PACKAGES] permissions.
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
		public val DEFAULT: InstallerType = if (areSplitPackagesSupported()) SESSION_BASED else INTENT_BASED
	}
}

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.LOLLIPOP)
@JvmSynthetic
internal fun areSplitPackagesSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP