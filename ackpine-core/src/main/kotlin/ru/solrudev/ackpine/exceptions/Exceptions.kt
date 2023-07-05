package ru.solrudev.ackpine.exceptions

import android.os.Build

/**
 * Thrown if installation of split packages is not supported when creating session with split package is attempted.
 */
public class SplitPackagesNotSupportedException : IllegalArgumentException(
	"Split packages are not supported on current Android API level: ${Build.VERSION.SDK_INT}"
)