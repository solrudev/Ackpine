package ru.solrudev.ackpine.session

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Awaits for the completion of the [Session] without blocking a thread and resumes when it's complete, returning the
 * resulting value or throwing the corresponding exception if the session was cancelled.
 *
 * This suspending function is cancellable.
 * If the [Job] of the current coroutine is cancelled or completed while this suspending function is waiting, this
 * function immediately resumes with [CancellationException].
 *
 * @return [SessionResult]
 */
public suspend fun <F : Failure> Session<F>.await(): SessionResult<F> = suspendCancellableCoroutine { continuation ->
	val subscription = addStateListener { _, state ->
		when (state) {
			Session.State.Pending -> launch()
			Session.State.Active -> launch()
			Session.State.Awaiting -> commit()
			Session.State.Committed -> {}
			Session.State.Cancelled -> continuation.cancel()
			Session.State.Succeeded -> continuation.resume(SessionResult.Success())
			is Session.State.Failed -> state.failure.let { failure ->
				if (failure is Failure.Exceptional) {
					continuation.resumeWithException(failure.exception)
				}
				continuation.resume(SessionResult.Error(failure))
			}
		}
	}
	continuation.invokeOnCancellation {
		subscription.dispose()
		cancel()
	}
}