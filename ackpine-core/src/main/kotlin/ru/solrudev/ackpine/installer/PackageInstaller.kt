package ru.solrudev.ackpine.installer

import com.google.common.util.concurrent.ListenableFuture
import ru.solrudev.ackpine.DisposableSubscription
import ru.solrudev.ackpine.installer.parameters.InstallParameters
import ru.solrudev.ackpine.session.Progress
import ru.solrudev.ackpine.session.Session
import java.util.UUID

public interface PackageInstaller {

	public fun createSession(parameters: InstallParameters): Session<InstallFailure>
	public fun getSession(sessionId: UUID): ListenableFuture<Session<InstallFailure>?>
	public fun addProgressListener(sessionId: UUID, listener: ProgressListener): DisposableSubscription
	public fun removeProgressListener(listener: ProgressListener)

	public fun interface ProgressListener {
		public fun onProgressChanged(sessionId: UUID, progress: Progress)
	}
}

internal class ProgressDisposableSubscription internal constructor(
	private var packageInstaller: PackageInstaller?,
	private var listener: PackageInstaller.ProgressListener?
) : DisposableSubscription {

	override var isDisposed: Boolean = false
		private set

	override fun dispose() {
		if (isDisposed) {
			return
		}
		val listener = this.listener
		if (listener != null) {
			packageInstaller?.removeProgressListener(listener)
		}
		this.listener = null
		packageInstaller = null
		isDisposed = true
	}
}