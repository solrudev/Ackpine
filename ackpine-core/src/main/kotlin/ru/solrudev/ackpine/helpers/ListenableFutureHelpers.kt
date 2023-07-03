package ru.solrudev.ackpine.helpers

import android.annotation.SuppressLint
import androidx.concurrent.futures.DirectExecutor
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutionException

@SuppressLint("RestrictedApi")
@JvmSynthetic
internal inline fun <V> ListenableFuture<V>.handleResult(crossinline block: (V) -> Unit) {
	if (isDone) {
		block(getAndUnwrapException())
		return
	}
	addListener({ block(getAndUnwrapException()) }, DirectExecutor.INSTANCE)
}

private fun <V> ListenableFuture<V>.getAndUnwrapException(): V {
	val value = try {
		get()
	} catch (e: ExecutionException) {
		throw e.cause ?: e
	}
	return value
}