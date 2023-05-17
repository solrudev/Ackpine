package ru.solrudev.ackpine

import java.util.UUID

public interface PackageUninstaller {
	public fun createSession(parameters: UninstallParameters): Session<UninstallFailure>
	public fun getSession(sessionId: UUID): Session<UninstallFailure>?
}