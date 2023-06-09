package ru.solrudev.ackpine.exceptions

import android.net.Uri
import android.os.Build

public class SplitPackagesNotSupportedException : IllegalArgumentException(
	"Split packages are not supported on current Android API level: ${Build.VERSION.SDK_INT}"
)

public class UnsupportedUriSchemeException(uri: Uri) : IllegalArgumentException(
	"Scheme of the provided URI is not supported: $uri"
)