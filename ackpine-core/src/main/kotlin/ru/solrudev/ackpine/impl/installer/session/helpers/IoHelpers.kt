package ru.solrudev.ackpine.impl.installer.session.helpers

import android.os.CancellationSignal
import java.io.InputStream
import java.io.OutputStream
import kotlin.math.roundToInt

private const val BUFFER_LENGTH = 8192

@get:JvmSynthetic
internal const val STREAM_COPY_PROGRESS_MAX: Int = 100

@JvmSynthetic
internal inline fun InputStream.copyTo(
	out: OutputStream,
	size: Long,
	signal: CancellationSignal,
	onProgress: (Int) -> Unit = {}
) {
	val progressRatio = (size.toDouble() / (BUFFER_LENGTH * STREAM_COPY_PROGRESS_MAX)).roundToInt().coerceAtLeast(1)
	val buffer = ByteArray(BUFFER_LENGTH)
	var currentProgress = 0
	var accumulatedBytesRead = 0
	var progressEmitCounter = 0
	while (true) {
		signal.throwIfCanceled()
		val bytesRead = read(buffer, 0, BUFFER_LENGTH - accumulatedBytesRead)
		if (bytesRead < 0) {
			break
		}
		accumulatedBytesRead += bytesRead
		out.write(buffer, 0, bytesRead)
		if (accumulatedBytesRead == BUFFER_LENGTH) {
			accumulatedBytesRead = 0
			val progress = ++currentProgress / progressRatio
			val shouldEmitProgress = currentProgress - (progress * progressRatio) == 0
			if (shouldEmitProgress && progress <= STREAM_COPY_PROGRESS_MAX) {
				progressEmitCounter++
				onProgress(1)
			}
		}
	}
	onProgress(STREAM_COPY_PROGRESS_MAX - progressEmitCounter)
}