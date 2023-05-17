package ru.solrudev.ackpine

import android.net.Uri

/**
 * Constructs a new instance of [InstallParameters].
 */
public inline fun InstallParameters(
	baseApk: Uri,
	initializer: InstallParameters.Builder.() -> Unit
): InstallParameters {
	return InstallParameters.Builder(baseApk).apply(initializer).build()
}