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

package ru.solrudev.ackpine.io

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import androidx.annotation.RequiresApi
import ru.solrudev.ackpine.helpers.closeWithException
import ru.solrudev.ackpine.helpers.entries
import ru.solrudev.ackpine.helpers.toFile
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

internal class ZipEntryStream private constructor(
	private val inputStream: InputStream,
	val size: Long,
	private vararg var resources: AutoCloseable
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
		for (resource in resources) {
			runCatching { resource.close() }
		}
		resources = emptyArray()
		inputStream.close()
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is ZipEntryStream) return false
		return inputStream == other.inputStream
	}

	override fun hashCode(): Int = inputStream.hashCode()
	override fun toString(): String = inputStream.toString()

	internal companion object {

		@JvmSynthetic
		internal fun open(
			uri: Uri,
			zipEntryName: String,
			context: Context,
			signal: CancellationSignal?
		): ZipEntryStream? {
			val file = uri.toFile(context, signal)
			return when {
				file.canRead() -> openZipEntryStreamUsingZipFile(file, zipEntryName)
				Build.VERSION.SDK_INT >= 26 -> openZipEntryStreamApi26(uri, zipEntryName, context, signal)
				else -> openZipEntryStreamUsingZipInputStream(uri, zipEntryName, context, signal)
			}
		}

		private fun openZipEntryStreamUsingZipFile(file: File, zipEntryName: String): ZipEntryStream? {
			val zipFile = ZipFile(file)
			try {
				val zipEntry = zipFile.getEntry(zipEntryName) ?: return null
				return ZipEntryStream(zipFile.getInputStream(zipEntry), zipEntry.size, zipFile)
			} catch (throwable: Throwable) {
				zipFile.closeWithException(throwable)
				throw throwable
			}
		}

		@RequiresApi(Build.VERSION_CODES.O)
		private fun openZipEntryStreamApi26(
			zipFileUri: Uri,
			zipEntryName: String,
			context: Context,
			signal: CancellationSignal?
		): ZipEntryStream? {
			var fd: ParcelFileDescriptor? = null
			var fileInputStream: FileInputStream? = null
			val zipFile: org.apache.commons.compress.archivers.zip.ZipFile
			try {
				fd = context.contentResolver.openFileDescriptor(zipFileUri, "r", signal)
					?: throw NullPointerException("ParcelFileDescriptor was null: $zipFileUri")
				fileInputStream = FileInputStream(fd.fileDescriptor)
				zipFile = org.apache.commons.compress.archivers.zip.ZipFile.builder()
					.setSeekableByteChannel(fileInputStream.channel)
					.get()
			} catch (throwable: Throwable) {
				fd?.closeWithException(throwable)
				fileInputStream?.closeWithException(throwable)
				throwable.printStackTrace()
				return openZipEntryStreamUsingZipInputStream(zipFileUri, zipEntryName, context, signal)
			}
			try {
				val zipEntry = zipFile.getEntry(zipEntryName) ?: return null
				return ZipEntryStream(zipFile.getInputStream(zipEntry), zipEntry.size, zipFile, fileInputStream, fd)
			} catch (throwable: Throwable) {
				fd.closeWithException(throwable)
				fileInputStream.closeWithException(throwable)
				zipFile.closeWithException(throwable)
				throw throwable
			}
		}

		private fun openZipEntryStreamUsingZipInputStream(
			zipFileUri: Uri,
			zipEntryName: String,
			context: Context,
			signal: CancellationSignal?
		): ZipEntryStream? {
			val zipStream = ZipInputStream(context.contentResolver.openInputStream(zipFileUri))
			val zipEntry = try {
				zipStream.entries()
					.onEach { signal?.throwIfCanceled() }
					.firstOrNull { it.name == zipEntryName } ?: return null
			} catch (throwable: Throwable) {
				zipStream.closeWithException(throwable)
				throw throwable
			}
			return ZipEntryStream(zipStream, zipEntry.size)
		}
	}
}