package ru.solrudev.ackpine.session

import ru.solrudev.ackpine.DisposableSubscription
import java.util.UUID

/**
 * A [Session] with a progress.
 */
public interface ProgressSession<out F : Failure> : Session<F> {

	/**
	 * Adds a [ProgressListener] to this session. The listener will be notified with current progress immediately upon
	 * registering.
	 *
	 * @return [DisposableSubscription]
	 */
	public fun addProgressListener(listener: ProgressListener): DisposableSubscription

	/**
	 * Removes the provided [ProgressListener] from this session.
	 */
	public fun removeProgressListener(listener: ProgressListener)

	/**
	 * Callback interface for listening to [ProgressSession] progress updates.
	 */
	public fun interface ProgressListener {

		/**
		 * Notifies about progress update.
		 * @param sessionId ID of the session which had its progress updated.
		 * @param progress progress of the session.
		 */
		public fun onProgressChanged(sessionId: UUID, progress: Progress)
	}
}