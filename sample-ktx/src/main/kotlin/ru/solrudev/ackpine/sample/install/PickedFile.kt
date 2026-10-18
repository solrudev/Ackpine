/*
 * Copyright (C) 2026 Ilya Fomichev
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

package ru.solrudev.ackpine.sample.install

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

internal const val APK_MIME_TYPE = "application/vnd.android.package-archive"
internal const val ZIP_MIME_TYPE = "application/zip"
internal const val BINARY_MIME_TYPE = "application/octet-stream"

private val ARCHIVE_TYPES = setOf("zip", "apks", "xapk", "apkm", ZIP_MIME_TYPE, BINARY_MIME_TYPE)

/**
 * A file picked by the user, classified by its extension, falling back to its MIME type.
 */
internal class PickedFile(val uri: Uri, val name: String, private val fileType: String?) {

	val isArchive: Boolean
		get() = fileType in ARCHIVE_TYPES

	/**
	 * A v4 signature is recognized by its extension only: it has no MIME type of its own, and document providers
	 * report it as [BINARY_MIME_TYPE], which is also used for archives.
	 */
	val isV4Signature: Boolean
		get() = name.endsWith(V4_SIGNATURE_EXTENSION, ignoreCase = true)
}

internal fun Uri.toPickedFile(context: Context): PickedFile {
	val name = DocumentFile.fromSingleUri(context, this)?.name.orEmpty()
	val fileType = name
		.substringAfterLast('.', "")
		.ifEmpty { context.contentResolver.getType(this) }
		?.lowercase()
	return PickedFile(this, name, fileType)
}
