package ru.solrudev.ackpine

import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Constructs a new instance of [InstallParameters].
 */
public inline fun InstallParameters(
	baseApk: Uri,
	initializer: InstallParameters.Builder.() -> Unit
): InstallParameters {
	return InstallParameters.Builder(baseApk).apply(initializer).build()
}

/**
 * Constructs a new instance of [InstallParameters].
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public inline fun InstallParameters(
	apks: Iterable<Uri>,
	initializer: InstallParameters.Builder.() -> Unit
): InstallParameters {
	return InstallParameters.Builder(apks).apply(initializer).build()
}