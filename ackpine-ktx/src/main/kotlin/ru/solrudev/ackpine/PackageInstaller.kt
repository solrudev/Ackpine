package ru.solrudev.ackpine

import android.net.Uri

/**
 * Creates an install session.
 *
 * Split packages are not supported on Android versions lower than Lollipop (5.0).
 * Attempting to add additional APKs on these versions will produce [SplitPackagesNotSupportedException].
 *
 * @see PackageInstaller.createSession
 * @param baseApk [Uri] of base APK.
 * @param parametersBuilder [InstallParameters] initializer.
 * @return [Session]
 */
public inline fun PackageInstaller.createSession(
	baseApk: Uri,
	parametersBuilder: InstallParameters.Builder.() -> Unit
): Session<InstallFailure> = createSession(InstallParameters(baseApk, parametersBuilder))