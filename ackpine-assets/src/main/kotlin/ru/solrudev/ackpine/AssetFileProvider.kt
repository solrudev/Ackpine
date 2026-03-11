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
import ru.solrudev.ackpine.helpers.use
import java.io.FileNotFoundException

/**
 * [ContentProvider] which allows to open asset files inside of application's package.
 *
 * Supports querying of assets' names and sizes.
 */
public class AssetFileProvider : ContentProvider() {

	override fun attachInfo(context: Context, info: ProviderInfo) {
		super.attachInfo(context, info)
		providerAuthority = info.authority
	}

	override fun onCreate(): Boolean = true

	override fun getType(uri: Uri): String? {
		return uri.toAssetPath()
			.substringAfterLast('/')
			.substringAfterLast('.', "")
			.let { extension -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) }
	}

	override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
		return openFile(uri, mode, signal = null)
	}

	override fun openFile(uri: Uri, mode: String, signal: CancellationSignal?): ParcelFileDescriptor? {
		checkWriteMode(mode)
		return uri.toAssetPath().let { path -> context?.assets?.openFd(path)?.parcelFileDescriptor }
	}

	override fun openAssetFile(uri: Uri, mode: String): AssetFileDescriptor? {
		return openAssetFile(uri, mode, signal = null)
	}

	override fun openAssetFile(uri: Uri, mode: String, signal: CancellationSignal?): AssetFileDescriptor? {
		checkWriteMode(mode)
		return uri.toAssetPath().let { path -> context?.assets?.openFd(path) }
	}

	override fun openTypedAssetFile(uri: Uri, mimeTypeFilter: String, opts: Bundle?): AssetFileDescriptor? {
		return openTypedAssetFile(uri, mimeTypeFilter, opts, null)
	}

	// Copied from default ContentProvider implementation, but here we use CancellationSignal
	override fun openTypedAssetFile(
		uri: Uri,
		mimeTypeFilter: String,
		opts: Bundle?,
		signal: CancellationSignal?
	): AssetFileDescriptor? {
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
		val assetPath = uri.toAssetPath()

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
		row.setColumn(OpenableColumns.DISPLAY_NAME, assetPath.substringAfterLast('/'))
		if (OpenableColumns.SIZE in columnNames) {
			openAssetFile(uri, "r", signal).use { assetFileDescriptor ->
				row.setColumn(OpenableColumns.SIZE, assetFileDescriptor?.declaredLength ?: -1L)
			}
		}
		cursor.addRow(row)
		return cursor
	}

	private fun checkWriteMode(mode: String) {
		if ('w' in mode || 'W' in mode) {
			throw UnsupportedOperationException("Write mode is not supported by AssetFileProvider")
		}
	}

	private fun Uri.toAssetPath() = when {
		authority != providerAuthority -> throw FileNotFoundException("uri=$this")
		isEncoded() -> parseEncodedUri(this)
		else -> parseLegacyUri(this)
	}

	private fun parseEncodedUri(uri: Uri): String {
		val assetPathSegments = uri.pathSegments
		if (assetPathSegments.isEmpty()) {
			throw FileNotFoundException("No asset path for uri=$uri")
		}
		return assetPathSegments.joinToString(separator = "/")
	}

	private fun parseLegacyUri(uri: Uri): String {
		return uri.encodedPath?.drop(1) ?: throw FileNotFoundException("uri=$uri")
	}

	override fun insert(uri: Uri, values: ContentValues?): Uri? {
		throw UnsupportedOperationException("Inserting not supported by AssetFileProvider")
	}

	override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
		throw UnsupportedOperationException("Deleting not supported by AssetFileProvider")
	}

	override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
		throw UnsupportedOperationException("Updating not supported by AssetFileProvider")
	}

	public companion object {

		private const val QUERY_PARAMETER_ENCODED = "_encoded"
		private const val QUERY_PARAMETER_TRUE = "1"
		private lateinit var providerAuthority: String

		/**
		 * Returns whether provided [uri] was created by [AssetFileProvider].
		 */
		@JvmStatic
		public fun isAssetFileProviderUri(uri: Uri): Boolean = when {
			uri.authority != providerAuthority -> false
			uri.isEncoded() -> true
			else -> uri.encodedPath != null
		}

		/**
		 * Creates an [Uri] for an asset file inside of `assets` folder in application's package.
		 * @param fileName the name of the asset. It can be hierarchical.
		 */
		@JvmStatic
		public fun getUriForAsset(fileName: String): Uri {
			require(fileName.isNotEmpty()) { "fileName cannot be empty" }
			return Uri.Builder()
				.scheme(ContentResolver.SCHEME_CONTENT)
				.authority(providerAuthority)
				.apply {
					fileName.split('/').forEach(::appendPath)
				}
				.appendQueryParameter(QUERY_PARAMETER_ENCODED, QUERY_PARAMETER_TRUE)
				.build()
		}

		private fun Uri.isEncoded(): Boolean {
			return getQueryParameter(QUERY_PARAMETER_ENCODED) == QUERY_PARAMETER_TRUE
		}
	}
}