package ru.solrudev.ackpine.session

import ru.solrudev.ackpine.DisposableSubscription
import java.util.UUID

public interface ProgressSession<out F : Failure> : Session<F> {

	public fun addProgressListener(listener: ProgressListener): DisposableSubscription
	public fun removeProgressListener(listener: ProgressListener)

	public fun interface ProgressListener {
		public fun onProgressChanged(sessionId: UUID, progress: Progress)
	}
}