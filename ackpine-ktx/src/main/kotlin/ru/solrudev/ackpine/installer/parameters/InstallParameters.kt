package ru.solrudev.ackpine.installer.parameters

import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Constructs a new instance of [InstallParameters].
 */
public inline fun InstallParameters(
	baseApk: Uri,
	configure: InstallParametersDsl.() -> Unit
): InstallParameters {
	return InstallParametersDslBuilder(baseApk).apply(configure).build()
}

/**
 * Constructs a new instance of [InstallParameters].
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public inline fun InstallParameters(
	apks: Iterable<Uri>,
	configure: InstallParametersDsl.() -> Unit
): InstallParameters {
	return InstallParametersDslBuilder(apks).apply(configure).build()
}