package ru.solrudev.ackpine.uninstaller

import ru.solrudev.ackpine.futures.await
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.uninstaller.parameters.UninstallParameters
import ru.solrudev.ackpine.uninstaller.parameters.UninstallParametersDsl
import java.util.UUID

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
	configure: UninstallParametersDsl.() -> Unit = {}
): Session<UninstallFailure> {
	return createSession(UninstallParameters(packageName, configure))
}

/**
 * A suspending variant of [PackageUninstaller.getSessionAsync].
 * @return [Session] or `null` if not found.
 */
public suspend inline fun PackageUninstaller.getSession(sessionId: UUID): Session<UninstallFailure>? {
	return getSessionAsync(sessionId).await()
}

/**
 * A suspending variant of [PackageUninstaller.getSessionsAsync].
 * @return [Session] or `null` if not found.
 */
public suspend inline fun PackageUninstaller.getSessions(): List<Session<UninstallFailure>> {
	return getSessionsAsync().await()
}

/**
 * A suspending variant of [PackageUninstaller.getActiveSessionsAsync].
 * @return [Session] or `null` if not found.
 */
public suspend inline fun PackageUninstaller.getActiveSessions(): List<Session<UninstallFailure>> {
	return getActiveSessionsAsync().await()
}