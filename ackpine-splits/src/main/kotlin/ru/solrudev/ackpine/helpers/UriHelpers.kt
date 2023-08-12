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

package ru.solrudev.ackpine.helpers

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.CancellationSignal
import android.os.Process
import android.provider.OpenableColumns
import java.io.File
import java.io.FileNotFoundException

@JvmSynthetic
internal fun Uri.toFile(context: Context, signal: CancellationSignal? = null): File {
	if (scheme == ContentResolver.SCHEME_FILE) {
		return File(requireNotNull(path) { "Uri path is null: $this" })
	}
	try {
		context.contentResolver.openFileDescriptor(this, "r", signal).use { fileDescriptor ->
			if (fileDescriptor == null) {
				throw NullPointerException("ParcelFileDescriptor was null: $this")
			}
			val path = "/proc/${Process.myPid()}/fd/${fileDescriptor.fd}"
			val canonicalPath = File(path).canonicalPath.replace("mnt/media_rw", "storage")
			return File(canonicalPath)
		}
	} catch (_: FileNotFoundException) {
		return File("")
	}
}

@JvmSynthetic
internal fun Uri.displayNameAndSize(context: Context): Pair<String, Long> {
	context.contentResolver.query(
		this,
		arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
		null, null, null
	).use { cursor ->
		cursor ?: return "" to -1L
		if (!cursor.moveToFirst()) {
			return "" to -1L
		}
		return cursor.getString(0).orEmpty() to cursor.getLong(1)
	}
}