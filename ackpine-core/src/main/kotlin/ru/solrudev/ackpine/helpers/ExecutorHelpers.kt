package ru.solrudev.ackpine.helpers

import android.annotation.SuppressLint
import androidx.concurrent.futures.ResolvableFuture
import java.util.concurrent.Executor

@SuppressLint("RestrictedApi")
@JvmSynthetic
internal inline fun <V> Executor.safeExecuteWith(future: ResolvableFuture<V>, crossinline command: () -> Unit) {
	try {
		execute {
			try {
				command()
			} catch (t: Throwable) {
				future.setException(t)
			}
		}
	} catch (t: Throwable) {
		future.setException(t)
	}
}