package ru.solrudev.ackpine

import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Creates an install session.
 *
 * Split packages are not supported on API levels < 21.
 * Attempting to add additional APKs on these API levels will produce [SplitPackagesNotSupportedException].
 *
 * @see PackageInstaller.createSession
 * @param baseApk [URI][Uri] of base APK.
 * @param parametersBuilder [InstallParameters] initializer.
 * @return [Session]
 */
public inline fun PackageInstaller.createSession(
	baseApk: Uri,
	parametersBuilder: InstallParameters.Builder.() -> Unit
): Session<InstallFailure> {
	return createSession(InstallParameters(baseApk, parametersBuilder))
}

/**
 * Creates an install session.
 *
 * Split packages are not supported on API levels < 21.
 * Attempting to add additional APKs on these API levels will produce [SplitPackagesNotSupportedException].
 *
 * @see PackageInstaller.createSession
 * @param apks [URIs][Uri] of split APKs.
 * @param parametersBuilder [InstallParameters] initializer.
 * @return [Session]
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public inline fun PackageInstaller.createSession(
	apks: Iterable<Uri>,
	parametersBuilder: InstallParameters.Builder.() -> Unit
): Session<InstallFailure> {
	return createSession(InstallParameters(apks, parametersBuilder))
}