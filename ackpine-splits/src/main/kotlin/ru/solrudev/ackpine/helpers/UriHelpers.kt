/*
 * Copyright (C) 2023-2024 Ilya Fomichev
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
import android.os.Environment
import android.os.Process
import android.provider.DocumentsContract
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
			val canonicalPath = File(path).canonicalPath.let { canonicalPath ->
				if (canonicalPath.startsWith("/mnt/media_rw")) {
					canonicalPath.replaceFirst("/mnt/media_rw", "/storage")
				} else {
					canonicalPath
				}
			}
			if (canonicalPath == path) {
				return tryFileFromExternalDocumentUri(context, this) ?: File("")
			}
			return File(canonicalPath)
		}
	} catch (_: FileNotFoundException) {
		return File("")
	}
}

private fun tryFileFromExternalDocumentUri(context: Context, uri: Uri): File? {
	if (!DocumentsContract.isDocumentUri(context, uri)) {
		return null
	}
	if (uri.authority != "com.android.externalstorage.documents") {
		return null
	}
	val documentId = DocumentsContract.getDocumentId(uri)
	val segments = documentId.split(':', limit = 2)
	val storageId = segments[0]
	if (storageId.lowercase() != "primary") {
		return File("storage/${documentId.replace(':', '/')}")
	}
	if (segments.size > 1) {
		val name = segments[1]
		return File("${Environment.getExternalStorageDirectory().absolutePath}/$name/")
	}
	return Environment.getExternalStorageDirectory()
}