package ru.solrudev.ackpine.installer.parameters

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.RequiresApi

/**
 * Type of the package installer implementation.
 */
public enum class InstallerType {
	INTENT_BASED,

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