package ru.solrudev.ackpine

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

public suspend fun <F : Failure> Session<F>.await(): SessionResult<F> = suspendCancellableCoroutine { continuation ->
	val subscription = addStateListener { _, state ->
		when (state) {
			Session.State.Active -> {}
			Session.State.Pending -> {}
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
	}
	launch()
}