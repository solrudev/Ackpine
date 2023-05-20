package ru.solrudev.ackpine

import android.os.Build

public class SplitPackagesNotSupportedException : IllegalArgumentException(
	"Split packages are not supported on current Android API level: ${Build.VERSION.SDK_INT}"
)