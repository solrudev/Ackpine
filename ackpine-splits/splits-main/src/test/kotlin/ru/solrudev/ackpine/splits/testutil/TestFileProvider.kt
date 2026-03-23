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

package ru.solrudev.ackpine.splits.testutil

import android.content.ContentProvider
import android.content.ContentValues
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import org.robolectric.Robolectric
import java.io.File
import java.io.FileNotFoundException
import java.io.OutputStream

class TestFileProvider : ContentProvider() {

	override fun onCreate() = true

	override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
		val file = resolveFile(uri)
		return ParcelFileDescriptor.open(file, ParcelFileDescriptor.parseMode(mode))
	}

	override fun query(
		uri: Uri,
		projection: Array<out String>?,
		selection: String?,
		selectionArgs: Array<out String>?,
		sortOrder: String?
	): Cursor {
		val columns = projection ?: arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
		val cursor = MatrixCursor(columns, 1)
		val file = resolveFile(uri)
		val row = arrayOfNulls<Any>(columns.size)
		for (i in columns.indices) {
			when (columns[i]) {
				OpenableColumns.DISPLAY_NAME -> row[i] = file.name
				OpenableColumns.SIZE -> row[i] = file.length()
			}
		}
		cursor.addRow(row)
		return cursor
	}

	override fun getType(uri: Uri) = MimeTypeMap.getSingleton().getMimeTypeFromExtension(resolveFile(uri).extension)
	override fun insert(uri: Uri, values: ContentValues?) = null
	override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
	override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0

	private fun resolveFile(uri: Uri): File {
		if (uri.authority != AUTHORITY) {
			throw FileNotFoundException("Not a TestFileProvider URI")
		}
		val filePath = uri.path ?: throw FileNotFoundException("No path in uri=$uri")
		val file = File(filePath)
		if (!file.exists()) {
			throw FileNotFoundException(filePath)
		}
		return file
	}

	companion object {

		private const val AUTHORITY = "ru.solrudev.ackpine.splits.test.file"

		fun setup() {
			Robolectric.buildContentProvider(TestFileProvider::class.java)
				.create(ProviderInfo().apply { authority = AUTHORITY })
		}

		fun getUri(file: File): Uri {
			val builder = Uri.Builder()
				.scheme("content")
				.authority(AUTHORITY)
			file.absolutePath
				.split(File.separatorChar)
				.filter { pathSegment -> pathSegment.isNotEmpty() }
				.forEach { pathSegment -> builder.appendPath(pathSegment) }
			return builder.build()
		}

		fun createSource(name: String, writer: (OutputStream) -> Unit): Uri {
			val tempFile = File.createTempFile("synthetic-", "-$name")
			tempFile.deleteOnExit()
			tempFile.outputStream().buffered().use { outputStream ->
				writer(outputStream)
				outputStream.flush()
			}
			return getUri(tempFile)
		}
	}
}