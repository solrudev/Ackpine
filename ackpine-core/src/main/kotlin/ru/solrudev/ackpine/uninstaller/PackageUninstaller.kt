package ru.solrudev.ackpine.uninstaller

import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.uninstaller.parameters.UninstallParameters
import java.util.UUID

public interface PackageUninstaller {
	public fun createSession(parameters: UninstallParameters): Session<UninstallFailure>
	public fun getSession(sessionId: UUID): Session<UninstallFailure>?
}