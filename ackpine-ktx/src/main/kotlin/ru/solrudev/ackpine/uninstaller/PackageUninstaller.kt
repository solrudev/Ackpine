package ru.solrudev.ackpine.uninstaller

import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.uninstaller.parameters.UninstallParameters
import ru.solrudev.ackpine.uninstaller.parameters.UninstallParametersDsl

/**
 * Creates an uninstall session.
 *
 * @see PackageUninstaller.createSession
 * @param packageName name of the package to be uninstalled.
 * @param configure configures [uninstall session][UninstallParametersDsl].
 * @return [Session]
 */
public inline fun PackageUninstaller.createSession(
	packageName: String,
	configure: UninstallParametersDsl.() -> Unit
): Session<UninstallFailure> {
	return createSession(UninstallParameters(packageName, configure))
}