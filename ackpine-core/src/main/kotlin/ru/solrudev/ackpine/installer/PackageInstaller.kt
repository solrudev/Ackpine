package ru.solrudev.ackpine.installer

import com.google.common.util.concurrent.ListenableFuture
import ru.solrudev.ackpine.DisposableSubscription
import ru.solrudev.ackpine.installer.parameters.InstallParameters
import ru.solrudev.ackpine.session.Progress
import ru.solrudev.ackpine.session.Session
import java.util.UUID

public interface PackageInstaller {

	public fun createSession(parameters: InstallParameters): Session<InstallFailure>
	public fun getSessionAsync(sessionId: UUID): ListenableFuture<Session<InstallFailure>?>
	public fun addProgressListener(sessionId: UUID, listener: ProgressListener): DisposableSubscription
	public fun removeProgressListener(listener: ProgressListener)

	public fun interface ProgressListener {
		public fun onProgressChanged(sessionId: UUID, progress: Progress)
	}
}