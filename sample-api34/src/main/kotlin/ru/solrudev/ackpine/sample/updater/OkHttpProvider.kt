/*
 * Copyright (C) 2024 Ilya Fomichev
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

package ru.solrudev.ackpine.sample.updater

import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.pm.ProviderInfo
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.net.Uri
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import androidx.core.net.toUri
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import kotlin.concurrent.thread

class OkHttpProvider : ContentProvider() {

	override fun attachInfo(context: Context, info: ProviderInfo) {
		super.attachInfo(context, info)
		authority = info.authority
	}

	override fun onCreate() = true

	override fun openAssetFile(uri: Uri, mode: String, signal: CancellationSignal?): AssetFileDescriptor {
		preparePipe(mode, signal) { inputFd, outputFd ->
			val response = executeHttpRequest(uri)
			val contentLength = response.headers["Content-Length"]?.toLongOrNull() ?: -1L
			response.writeBodyTo(outputFd, signal)
			return AssetFileDescriptor(inputFd, 0, contentLength)
		}
	}

	override fun openAssetFile(uri: Uri, mode: String): AssetFileDescriptor {
		return openAssetFile(uri, mode, signal = null)
	}

	override fun getType(uri: Uri) = executeHttpRequest(uri).use { response ->
		response.body.contentType()?.toString()
	}

	private fun executeHttpRequest(uri: Uri): Response {
		val url = Uri.Builder()
			.scheme("https")
			.authority(uri.encodedPath?.drop(1)?.substringBefore('/'))
			.encodedPath(uri.encodedPath?.drop(1)?.substringAfter('/'))
			.encodedQuery(uri.encodedQuery)
			.build()
			.toString()
		val request = Request(url.toHttpUrl())
		return okHttpClient.newCall(request).execute()
	}

	private fun Response.writeBodyTo(outputFd: ParcelFileDescriptor, signal: CancellationSignal?) = thread {
		use {
			outputFd.safeWrite { outputStream ->
				body.byteStream().buffered().use { inputStream ->
					inputStream.copyTo(outputStream, signal)
					outputStream.flush()
				}
			}
		}
	}

	private inline fun <R> preparePipe(
		mode: String,
		signal: CancellationSignal?,
		block: (inputFd: ParcelFileDescriptor, outputFd: ParcelFileDescriptor) -> R
	): R {
		try {
			if ('w' in mode || 'W' in mode) {
				throw UnsupportedOperationException("Write mode is not supported by OkHttpProvider")
			}
			val (inputFd, outputFd) = ParcelFileDescriptor.createReliablePipe()
			return block(inputFd, outputFd)
		} finally {
			signal?.throwIfCanceled()
		}
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
					closeWithError(exception.message)
				} else {
					close()
				}
			} catch (throwable: Throwable) {
				exception?.addSuppressed(throwable)
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

	override fun query(
		uri: Uri,
		projection: Array<out String>?,
		selection: String?,
		selectionArgs: Array<out String>?,
		sortOrder: String?
	): Cursor? {
		return null
	}

	override fun insert(uri: Uri, values: ContentValues?): Uri? {
		return null
	}

	override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
		return 0
	}

	override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
		return 0
	}

	companion object {

		private lateinit var authority: String
		private val okHttpClient = OkHttpClient.Builder().build()

		fun getUriForUrl(url: String): Uri {
			val parsedUrl = url.toUri()
			return Uri.Builder()
				.scheme(ContentResolver.SCHEME_CONTENT)
				.authority(authority)
				.encodedPath(parsedUrl.authority)
				.appendEncodedPath(parsedUrl.encodedPath?.drop(1))
				.encodedQuery(parsedUrl.encodedQuery)
				.build()
		}
	}
}