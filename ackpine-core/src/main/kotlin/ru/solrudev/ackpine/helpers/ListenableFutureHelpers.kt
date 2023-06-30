package ru.solrudev.ackpine.helpers

import android.annotation.SuppressLint
import androidx.concurrent.futures.DirectExecutor
import com.google.common.util.concurrent.ListenableFuture

@SuppressLint("RestrictedApi")
@JvmSynthetic
internal inline fun <V> ListenableFuture<V>.handleResult(crossinline block: (V) -> Unit) {
	if (isDone) {
		block(get())
	}
	addListener({ block(get()) }, DirectExecutor.INSTANCE)
}