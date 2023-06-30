package ru.solrudev.ackpine.futures

import android.annotation.SuppressLint
import androidx.concurrent.futures.DirectExecutor
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Awaits for the completion of the [ListenableFuture] without blocking a thread.
 * @return [R] - the result from the [ListenableFuture]
 */
@Suppress("BlockingMethodInNonBlockingContext")
@SuppressLint("RestrictedApi")
@PublishedApi
internal suspend inline fun <R> ListenableFuture<R>.await(): R {
	if (isDone) {
		try {
			return get()
		} catch (e: ExecutionException) {
			throw e.cause ?: e
		}
	}
	return suspendCancellableCoroutine { continuation ->
		addListener(
			{
				try {
					continuation.resume(get())
				} catch (throwable: Throwable) {
					val cause = throwable.cause ?: throwable
					when (throwable) {
						is CancellationException -> continuation.cancel(cause)
						else -> continuation.resumeWithException(cause)
					}
				}
			},
			DirectExecutor.INSTANCE
		)
		continuation.invokeOnCancellation {
			cancel(false)
		}
	}
}