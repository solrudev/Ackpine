package ru.solrudev.ackpine

import java.util.UUID

public interface PackageInstaller {

	public fun createSession(parameters: InstallParameters): Session<InstallFailure>
	public fun getSession(sessionId: UUID): Session<InstallFailure>?
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