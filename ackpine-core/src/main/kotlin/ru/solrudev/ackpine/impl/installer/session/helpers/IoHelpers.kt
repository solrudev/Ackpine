/*
 * Copyright (C) 2023 Ilya Fomichev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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