package ru.solrudev.ackpine.installer

import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import ru.solrudev.ackpine.exceptions.SplitPackagesNotSupportedException
import ru.solrudev.ackpine.installer.parameters.InstallParameters
import ru.solrudev.ackpine.installer.parameters.InstallParametersDsl
import ru.solrudev.ackpine.session.Session

/**
 * Creates an install session.
 *
 * Split packages are not supported on API levels < 21.
 * Attempting to add additional APKs on these API levels will produce [SplitPackagesNotSupportedException].
 *
 * @see PackageInstaller.createSession
 * @param baseApk [URI][Uri] of base APK.
 * @param configure configures [install session][InstallParametersDsl].
 * @return [Session]
 */
public inline fun PackageInstaller.createSession(
	baseApk: Uri,
	configure: InstallParametersDsl.() -> Unit
): Session<InstallFailure> {
	return createSession(InstallParameters(baseApk, configure))
}

/**
 * Creates an install session.
 *
 * Split packages are not supported on API levels < 21.
 * Attempting to add additional APKs on these API levels will produce [SplitPackagesNotSupportedException].
 *
 * @see PackageInstaller.createSession
 * @param apks [URIs][Uri] of split APKs.
 * @param configure configures [install session][InstallParametersDsl].
 * @return [Session]
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public inline fun PackageInstaller.createSession(
	apks: Iterable<Uri>,
	configure: InstallParametersDsl.() -> Unit
): Session<InstallFailure> {
	return createSession(InstallParameters(apks, configure))
}