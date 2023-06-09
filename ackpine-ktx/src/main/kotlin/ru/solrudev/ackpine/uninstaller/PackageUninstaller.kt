package ru.solrudev.ackpine.uninstaller

import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.uninstaller.parameters.UninstallParameters

/**
 * Creates an uninstall session.
 *
 * @see PackageUninstaller.createSession
 * @param packageName name of the package to be uninstalled.
 * @param parametersBuilder [UninstallParameters] initializer.
 * @return [Session]
 */
public inline fun PackageUninstaller.createSession(
	packageName: String,
	parametersBuilder: UninstallParameters.Builder.() -> Unit
): Session<UninstallFailure> {
	return createSession(UninstallParameters(packageName, parametersBuilder))
}