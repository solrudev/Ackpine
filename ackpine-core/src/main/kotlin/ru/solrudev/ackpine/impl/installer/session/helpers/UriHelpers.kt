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

import android.content.ContentResolver
import android.content.Context
import android.content.res.AssetFileDescriptor
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import android.os.Process
import android.provider.OpenableColumns
import java.io.File
import java.io.FileNotFoundException

@JvmSynthetic
internal fun Uri.toFile(context: Context, signal: CancellationSignal): File {
	if (scheme == ContentResolver.SCHEME_FILE) {
		return File(requireNotNull(path) { "Uri path is null: $this" })
	}
	try {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			context.contentResolver.openFileDescriptor(this, "r", signal)
		} else {
			context.contentResolver.openFileDescriptor(this, "r")
		}.use { fileDescriptor ->
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
internal fun Context.openAssetFileDescriptor(uri: Uri, signal: CancellationSignal): AssetFileDescriptor? {
	val afd = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
		contentResolver.openAssetFileDescriptor(uri, "r", signal)
	} else {
		contentResolver.openAssetFileDescriptor(uri, "r")
	}
	if (afd == null || afd.declaredLength != -1L) {
		return afd
	}
	return AssetFileDescriptor(afd.parcelFileDescriptor, afd.startOffset, contentResolver.getSize(uri, signal))
}

private fun ContentResolver.getSize(uri: Uri, signal: CancellationSignal): Long {
	query(uri, arrayOf(OpenableColumns.SIZE), null, null, null, signal).use { cursor ->
		cursor ?: return -1L
		if (!cursor.moveToFirst()) {
			return -1L
		}
		return try {
			cursor.getLong(0)
		} catch (_: Throwable) {
			-1L
		}
	}
}