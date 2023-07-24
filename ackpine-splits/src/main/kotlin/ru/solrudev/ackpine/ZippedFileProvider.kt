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

package ru.solrudev.ackpine

import android.content.ClipDescription
import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.pm.ProviderInfo
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.annotation.RestrictTo
import ru.solrudev.ackpine.helpers.entries
import ru.solrudev.ackpine.helpers.toFile
import ru.solrudev.ackpine.helpers.toUri
import ru.solrudev.ackpine.plugin.AckpinePlugin
import ru.solrudev.ackpine.plugin.AckpinePluginRegistry
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.Executor
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

/**
 * [ContentProvider] which allows to open files inside of ZIP archives.
 *
 * Supports querying of ZIP entries' names and sizes.
 */
public class ZippedFileProvider : ContentProvider() {

	override fun attachInfo(context: Context, info: ProviderInfo) {
		super.attachInfo(context, info)
		authority = info.authority
	}

	override fun onCreate(): Boolean {
		AckpinePluginRegistry.register(ZippedFileProvider)
		return true
	}

	override fun getType(uri: Uri): String? {
		return uri.encodedQuery
			?.substringAfterLast('.', "")
			?.let { extension -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) }
	}

	override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
		return openFile(uri, mode, signal = null)
	}

	override fun openFile(uri: Uri, mode: String, signal: CancellationSignal?): ParcelFileDescriptor {
		preparePipe(mode, signal) { inputFd, outputFd ->
			openZipEntry(uri, outputFd, signal)
			return inputFd
		}
	}

	override fun openAssetFile(uri: Uri, mode: String): AssetFileDescriptor {
		return openAssetFile(uri, mode, signal = null)
	}

	override fun openAssetFile(uri: Uri, mode: String, signal: CancellationSignal?): AssetFileDescriptor {
		preparePipe(mode, signal) { inputFd, outputFd ->
			val size = openZipEntry(uri, outputFd, signal)
			return AssetFileDescriptor(inputFd, 0, size)
		}
	}

	override fun openTypedAssetFile(uri: Uri, mimeTypeFilter: String, opts: Bundle?): AssetFileDescriptor {
		return openTypedAssetFile(uri, mimeTypeFilter, opts, null)
	}

	// Copied from default ContentProvider implementation, but here we use CancellationSignal
	override fun openTypedAssetFile(
		uri: Uri,
		mimeTypeFilter: String,
		opts: Bundle?,
		signal: CancellationSignal?
	): AssetFileDescriptor {
		if ("*/*" == mimeTypeFilter) {
			return openAssetFile(uri, "r", signal)
		}
		val baseType = getType(uri)
		if (baseType != null && ClipDescription.compareMimeTypes(baseType, mimeTypeFilter)) {
			return openAssetFile(uri, "r", signal)
		}
		throw FileNotFoundException("Can't open $uri as type $mimeTypeFilter")
	}

	override fun query(
		uri: Uri,
		projection: Array<out String>?,
		selection: String?,
		selectionArgs: Array<out String>?,
		sortOrder: String?
	): Cursor {
		return query(uri, projection, queryArgs = null, signal = null)
	}

	override fun query(
		uri: Uri,
		projection: Array<out String>?,
		selection: String?,
		selectionArgs: Array<out String>?,
		sortOrder: String?,
		signal: CancellationSignal?
	): Cursor {
		return query(uri, projection, queryArgs = null, signal)
	}

	override fun query(
		uri: Uri,
		projection: Array<out String>?,
		queryArgs: Bundle?,
		signal: CancellationSignal?
	): Cursor {
		val columnNames = projection ?: arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)

		fun Array<Any?>.setColumn(columnName: String, value: Any?) {
			val index = columnNames.indexOf(columnName)
			if (index != -1) {
				set(index, value)
			}
		}

		val cursor = MatrixCursor(columnNames, 1)
		if (columnNames.isEmpty()) {
			return cursor
		}
		val row = arrayOfNulls<Any>(columnNames.size)
		row.setColumn(OpenableColumns.DISPLAY_NAME, uri.encodedQuery)
		if (OpenableColumns.SIZE in columnNames) {
			openZipEntryStream(uri, signal).use { zipStream ->
				row.setColumn(OpenableColumns.SIZE, zipStream.size)
			}
		}
		cursor.addRow(row)
		return cursor
	}

	private inline fun <R> preparePipe(
		mode: String,
		signal: CancellationSignal?,
		block: (inputFd: ParcelFileDescriptor, outputFd: ParcelFileDescriptor) -> R
	): R {
		try {
			if ('w' in mode || 'W' in mode) {
				throw UnsupportedOperationException("Write mode is not supported by ZippedFileProvider")
			}
			val (inputFd, outputFd) = ParcelFileDescriptor.createReliablePipe()
			return block(inputFd, outputFd)
		} finally {
			signal?.throwIfCanceled()
		}
	}

	/**
	 * @return zip entry size.
	 */
	private fun openZipEntry(uri: Uri, outputFd: ParcelFileDescriptor, signal: CancellationSignal?): Long {
		val zipStream = openZipEntryStream(uri, signal)
		val size = zipStream.size
		executor.execute {
			outputFd.safeWrite { outputStream ->
				zipStream.buffered().use { zipStream ->
					zipStream.copyTo(outputStream, signal)
					outputStream.flush()
				}
			}
		}
		return size
	}

	private fun openZipEntryStream(uri: Uri, signal: CancellationSignal?): ZipEntryStream {
		val zipFileUri = zipFileUri(uri)
		val file = context?.let { context -> zipFileUri.toFile(context, signal) }
		if (file?.canRead() == true) {
			val zipFile = ZipFile(file)
			return try {
				val zipEntry = zipFile.getEntry(uri.encodedQuery)
				ZipEntryStream(zipFile, zipFile.getInputStream(zipEntry), zipEntry.size)
			} catch (t: Throwable) {
				zipFile.close()
				throw t
			}
		}
		// fall back to ZipInputStream if file is not readable directly
		val zipStream = ZipInputStream(context?.contentResolver?.openInputStream(zipFileUri))
		val zipEntry = try {
			zipStream.entries()
				.onEach { signal?.throwIfCanceled() }
				.first { it.name == uri.encodedQuery }
		} catch (t: Throwable) {
			zipStream.close()
			throw t
		}
		return ZipEntryStream(zipFile = null, zipStream, zipEntry.size)
	}

	private fun zipFileUri(uri: Uri): Uri {
		val path = uri.encodedPath?.drop(1) ?: throw FileNotFoundException("uri=$uri")
		val uriFromPath = Uri.parse(path)
		if (uriFromPath.scheme == null || uriFromPath.scheme == ContentResolver.SCHEME_FILE) {
			return Uri.fromFile(File("/$path"))
		}
		val encodedSchemeSpecificPart = uriFromPath.encodedSchemeSpecificPart
		val authorityIndex = encodedSchemeSpecificPart.indexOfFirst { it != '/' }
		val opaquePart = "//${encodedSchemeSpecificPart.substring(authorityIndex)}"
		return Uri.Builder()
			.scheme(uriFromPath.scheme)
			.encodedOpaquePart(opaquePart)
			.encodedFragment(uriFromPath.encodedFragment)
			.build() // creates opaque uri
			.toString().toUri() // converting to hierarchical uri
	}

	private inline fun ParcelFileDescriptor.safeWrite(block: (outputStream: OutputStream) -> Unit) {
		var exception: Throwable? = null
		try {
			FileOutputStream(fileDescriptor).buffered().use(block)
		} catch (t: Throwable) {
			exception = t
		} finally {
			try {
				if (exception != null) {
					closeWithError(exception.message)
				} else {
					close()
				}
			} catch (t: Throwable) {
				exception?.addSuppressed(t)
				exception?.printStackTrace()
			}
		}
	}

	private fun InputStream.copyTo(out: OutputStream, signal: CancellationSignal?) {
		val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
		var bytesRead = read(buffer)
		while (bytesRead >= 0 && (signal == null || !signal.isCanceled)) {
			out.write(buffer, 0, bytesRead)
			bytesRead = read(buffer)
		}
	}

	override fun insert(uri: Uri, values: ContentValues?): Uri? {
		throw UnsupportedOperationException("Inserting not supported by ZippedFileProvider")
	}

	override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
		throw UnsupportedOperationException("Deleting not supported by ZippedFileProvider")
	}

	override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
		throw UnsupportedOperationException("Updating not supported by ZippedFileProvider")
	}

	public companion object : AckpinePlugin {

		private lateinit var executor: Executor
		private lateinit var authority: String

		/**
		 * Returns whether provided [uri] was created by [ZippedFileProvider].
		 */
		@JvmStatic
		public fun isZippedFileProviderUri(uri: Uri): Boolean {
			return uri.authority == authority && uri.encodedPath != null && uri.encodedQuery != null
		}

		/**
		 * Creates an [Uri] for a ZIP entry.
		 *
		 * @param zipPath string representation of [Uri] pointing to a ZIP file containing [zip entry][zipEntryName] or
		 * absolute path to the ZIP file containing zip entry.
		 * @param zipEntryName name of the ZIP entry inside of the ZIP archive.
		 */
		@JvmStatic
		public fun getUriForZipEntry(zipPath: String, zipEntryName: String): Uri {
			return Uri.Builder()
				.scheme(ContentResolver.SCHEME_CONTENT)
				.authority(authority)
				.encodedPath(zipPath)
				.encodedQuery(zipEntryName)
				.build()
		}

		@RestrictTo(RestrictTo.Scope.LIBRARY)
		@JvmSynthetic
		override fun setExecutor(executor: Executor) {
			this.executor = executor
		}
	}
}

private class ZipEntryStream(
	private val zipFile: ZipFile?,
	private val inputStream: InputStream,
	val size: Long
) : InputStream() {

	override fun read(): Int = inputStream.read()
	override fun available(): Int = inputStream.available()
	override fun markSupported(): Boolean = inputStream.markSupported()
	override fun mark(readlimit: Int) = inputStream.mark(readlimit)
	override fun read(b: ByteArray?): Int = inputStream.read(b)
	override fun read(b: ByteArray?, off: Int, len: Int): Int = inputStream.read(b, off, len)
	override fun reset() = inputStream.reset()
	override fun skip(n: Long): Long = inputStream.skip(n)

	override fun close() {
		zipFile?.close()
		inputStream.close()
	}

	override fun hashCode(): Int = inputStream.hashCode()
	override fun equals(other: Any?): Boolean = inputStream == other
	override fun toString(): String = inputStream.toString()
}