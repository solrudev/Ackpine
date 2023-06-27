package ru.solrudev.ackpine.installer

import com.google.common.util.concurrent.ListenableFuture
import ru.solrudev.ackpine.installer.parameters.InstallParameters
import ru.solrudev.ackpine.session.ProgressSession
import java.util.UUID

public interface PackageInstaller {
	public fun createSession(parameters: InstallParameters): ProgressSession<InstallFailure>
	public fun getSessionAsync(sessionId: UUID): ListenableFuture<ProgressSession<InstallFailure>?>
}