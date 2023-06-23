package ru.solrudev.ackpine.uninstaller

import com.google.common.util.concurrent.ListenableFuture
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.uninstaller.parameters.UninstallParameters
import java.util.UUID

public interface PackageUninstaller {
	public fun createSession(parameters: UninstallParameters): Session<UninstallFailure>
	public fun getSessionAsync(sessionId: UUID): ListenableFuture<Session<UninstallFailure>?>
}