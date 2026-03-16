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
import androidx.core.net.toUri
import ru.solrudev.ackpine.helpers.closeWithException
import ru.solrudev.ackpine.io.ZipEntryStream
import ru.solrudev.ackpine.io.ZipEntryStreamException
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * [ContentProvider] which allows to open files inside of ZIP archives.
 *
 * Supports querying of ZIP entries' names and sizes.
 */
public class ZippedFileProvider : ContentProvider() {

	override fun attachInfo(context: Context, info: ProviderInfo) {
		super.attachInfo(context, info)
		providerAuthority = info.authority
	}

	override fun onCreate(): Boolean {
		return true
	}

	override fun getType(uri: Uri): String? {
		return uri.toZipEntryUri()
			.entryName
			.substringAfterLast('.', missingDelimiterValue = "")
			.let { extension -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) }
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
		val zipEntryUri = uri.toZipEntryUri()

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
		row.setColumn(OpenableColumns.DISPLAY_NAME, zipEntryUri.entryName)
		if (OpenableColumns.SIZE in columnNames) {
			openZipEntryStream(zipEntryUri, signal).use { zipStream ->
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
		if ('w' in mode || 'W' in mode) {
			throw UnsupportedOperationException("Write mode is not supported by ZippedFileProvider")
		}
		val (inputFd, outputFd) = ParcelFileDescriptor.createReliablePipe()
		return try {
			try {
				block(inputFd, outputFd)
			} finally {
				signal?.throwIfCanceled()
			}
		} catch (throwable: Throwable) {
			inputFd.closeWithFailure(throwable)
			outputFd.closeWithFailure(throwable)
			throw throwable
		}
	}

	/**
	 * @return zip entry size.
	 */
	private fun openZipEntry(uri: Uri, outputFd: ParcelFileDescriptor, signal: CancellationSignal?): Long {
		val zipEntryStream = openZipEntryStream(uri.toZipEntryUri(), signal)
		val size = zipEntryStream.size
		try {
			AckpineThreadPool.execute {
				zipEntryStream.use {
					outputFd.safeWrite { outputStream ->
						zipEntryStream.buffered().copyTo(outputStream, signal)
						outputStream.flush()
					}
				}
			}
		} catch (throwable: Throwable) {
			zipEntryStream.closeWithException(throwable)
			throw throwable
		}
		return size
	}

	private fun openZipEntryStream(uri: ZipEntryUri, signal: CancellationSignal?): ZipEntryStream {
		try {
			return ZipEntryStream.open(uri.zipFileUri, uri.entryName, context!!, signal)
				?: throw IOException("Zip entry ${uri.entryName} not found at ${uri.zipFileUri}")
		} catch (exception: ZipEntryStreamException) {
			throw IOException("Failed to open ZIP source at ${uri.zipFileUri}", exception)
		}
	}

	private fun Uri.toZipEntryUri() = when {
		authority != providerAuthority -> throw FileNotFoundException("uri=$this")
		isV2() -> parseV2Uri(this)
		else -> parseLegacyUri(this)
	}

	private fun parseV2Uri(uri: Uri): ZipEntryUri {
		val source = uri.getQueryParameter(QUERY_PARAMETER_SOURCE)
			?: throw FileNotFoundException("No source for uri=$uri")
		val zipEntryName = uri.getQueryParameter(QUERY_PARAMETER_ENTRY)
			?: throw FileNotFoundException("No entry for uri=$uri")
		return ZipEntryUri(source.toZipFileUri(), zipEntryName)
	}

	private fun parseLegacyUri(uri: Uri): ZipEntryUri {
		val zipEntryName = uri.encodedQuery ?: throw FileNotFoundException("uri=$uri")
		return ZipEntryUri(legacyZipFileUri(uri), zipEntryName)
	}

	private fun legacyZipFileUri(uri: Uri): Uri {
		val path = uri.encodedPath?.drop(1) ?: throw FileNotFoundException("uri=$uri")
		val uriFromPath = path.toUri()
		if (uriFromPath.scheme == null) {
			return File("/$path").toUri()
		}
		val opaquePart = buildString {
			if (uriFromPath.scheme == ContentResolver.SCHEME_FILE) {
				append('/')
			}
			append("//")
			val encodedSchemeSpecificPart = uriFromPath.encodedSchemeSpecificPart
			val firstNonSlashCharIndex = encodedSchemeSpecificPart.indexOfFirst { it != '/' }
			append(encodedSchemeSpecificPart.substring(firstNonSlashCharIndex))
		}
		return Uri.Builder()
			.scheme(uriFromPath.scheme)
			.encodedOpaquePart(opaquePart)
			.encodedFragment(uriFromPath.encodedFragment)
			.build() // creates opaque uri
			.toString().toUri() // converting to hierarchical uri
	}

	private fun String.toZipFileUri(): Uri {
		val uri = toUri()
		if (uri.scheme != null) {
			return uri
		}
		return File(this).toUri()
	}

	private inline fun ParcelFileDescriptor.safeWrite(block: (outputStream: OutputStream) -> Unit) {
		var exception: Throwable? = null
		try {
			FileOutputStream(fileDescriptor).buffered().use(block)
		} catch (throwable: Throwable) {
			exception = throwable
		} finally {
			try {
				if (exception != null) {
					closeWithError(exception.message ?: exception.javaClass.name)
				} else {
					close()
				}
			} catch (throwable: Throwable) {
				exception?.addSuppressed(throwable)
				exception?.printStackTrace()
			}
		}
	}

	private fun ParcelFileDescriptor.closeWithFailure(throwable: Throwable) {
		try {
			closeWithError(throwable.message ?: throwable.javaClass.name)
		} catch (closeException: Throwable) {
			throwable.addSuppressed(closeException)
		}
	}

	private fun InputStream.copyTo(out: OutputStream, signal: CancellationSignal?) {
		val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
		var bytesRead = read(buffer)
		while (bytesRead >= 0) {
			signal?.throwIfCanceled()
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

	private class ZipEntryUri(
		val zipFileUri: Uri,
		val entryName: String
	)

	public companion object {

		private const val CURRENT_URI_VERSION = "v2"
		private const val QUERY_PARAMETER_SOURCE = "source"
		private const val QUERY_PARAMETER_ENTRY = "entry"
		private lateinit var providerAuthority: String

		/**
		 * Returns whether provided [uri] was created by [ZippedFileProvider].
		 */
		@JvmStatic
		public fun isZippedFileProviderUri(uri: Uri): Boolean = when {
			uri.authority != providerAuthority -> false
			uri.isV2() -> true
			else -> uri.encodedPath != null && uri.encodedQuery != null
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
				.authority(providerAuthority)
				.appendPath(CURRENT_URI_VERSION)
				.appendQueryParameter(QUERY_PARAMETER_SOURCE, zipPath)
				.appendQueryParameter(QUERY_PARAMETER_ENTRY, zipEntryName)
				.build()
		}

		/**
		 * Creates an [Uri] for a ZIP entry.
		 *
		 * @param file a ZIP file containing [zip entry][zipEntryName].
		 * @param zipEntryName name of the ZIP entry inside of the ZIP archive.
		 */
		@JvmStatic
		public fun getUriForZipEntry(file: File, zipEntryName: String): Uri {
			return getUriForZipEntry(file.absolutePath, zipEntryName)
		}

		/**
		 * Creates an [Uri] for a ZIP entry.
		 *
		 * @param uri [Uri] pointing to a ZIP file containing [zip entry][zipEntryName].
		 * @param zipEntryName name of the ZIP entry inside of the ZIP archive.
		 */
		@JvmStatic
		public fun getUriForZipEntry(uri: Uri, zipEntryName: String): Uri {
			return getUriForZipEntry(uri.toString(), zipEntryName)
		}

		private fun Uri.isV2() = pathSegments.firstOrNull() == CURRENT_URI_VERSION
				&& getQueryParameter(QUERY_PARAMETER_SOURCE) != null
				&& getQueryParameter(QUERY_PARAMETER_ENTRY) != null
	}
}