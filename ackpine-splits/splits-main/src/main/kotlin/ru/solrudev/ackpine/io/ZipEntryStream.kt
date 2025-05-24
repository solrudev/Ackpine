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
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import ru.solrudev.ackpine.helpers.closeAll
import ru.solrudev.ackpine.helpers.closeWithException
import ru.solrudev.ackpine.helpers.entries
import ru.solrudev.ackpine.helpers.getFileFromUri
import java.io.File
import java.io.FileInputStream
import java.io.FilterInputStream
import java.io.InputStream
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

internal class ZipEntryStream private constructor(
	private val inputStream: InputStream,
	val size: Long,
	private vararg var resources: AutoCloseable
) : FilterInputStream(inputStream) {

	override fun close() {
		try {
			closeAll(inputStream, *resources)
		} finally {
			resources = emptyArray()
		}
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is ZipEntryStream) return false
		if (inputStream != other.inputStream) return false
		if (size != other.size) return false
		return resources.contentEquals(other.resources)
	}

	override fun hashCode(): Int {
		var result = inputStream.hashCode()
		result = 31 * result + size.hashCode()
		result = 31 * result + resources.contentHashCode()
		return result
	}

	override fun toString(): String {
		return "ZipEntryStream(" +
				"inputStream=$inputStream, " +
				"size=$size, " +
				"resources=${resources.contentToString()}" +
				")"
	}

	internal companion object {

		@JvmSynthetic
		internal fun open(
			uri: Uri,
			zipEntryName: String,
			context: Context,
			signal: CancellationSignal? = null
		): ZipEntryStream? {
			val file = context.getFileFromUri(uri, signal)
			if (file.canRead()) {
				return openUsingZipFile(file, zipEntryName)
			}
			return openUsingFileChannel(uri, zipEntryName, context, signal)
		}

		private fun openUsingZipFile(file: File, zipEntryName: String): ZipEntryStream? {
			val zipFile = ZipFile(file)
			try {
				val zipEntry = zipFile.getEntry(zipEntryName)
				if (zipEntry == null) {
					zipFile.close()
					return null
				}
				return ZipEntryStream(zipFile.getInputStream(zipEntry), zipEntry.size, zipFile)
			} catch (throwable: Throwable) {
				zipFile.closeWithException(throwable)
				throw throwable
			}
		}

		private fun openUsingFileChannel(
			uri: Uri,
			zipEntryName: String,
			context: Context,
			signal: CancellationSignal?
		): ZipEntryStream? {
			var fd: ParcelFileDescriptor? = null
			var fileInputStream: FileInputStream? = null
			val zipFile: ru.solrudev.ackpine.compress.archivers.zip.ZipFile
			try {
				fd = context.contentResolver.openFileDescriptor(uri, "r", signal)
					?: throw NullPointerException("ParcelFileDescriptor was null: $uri")
				fileInputStream = FileInputStream(fd.fileDescriptor)
				zipFile = ru.solrudev.ackpine.compress.archivers.zip.ZipFile.builder()
					.setFileChannel(fileInputStream.channel)
					.get()
			} catch (throwable: Throwable) {
				fd?.closeWithException(throwable)
				fileInputStream?.closeWithException(throwable)
				throwable.printStackTrace()
				return openUsingZipInputStream(uri, zipEntryName, context, signal)
			}
			try {
				val zipEntry = zipFile.getEntry(zipEntryName)
				if (zipEntry == null) {
					closeAll(fd, fileInputStream, zipFile)
					return null
				}
				return ZipEntryStream(zipFile.getInputStream(zipEntry), zipEntry.size, zipFile, fileInputStream, fd)
			} catch (throwable: Throwable) {
				fd.closeWithException(throwable)
				fileInputStream.closeWithException(throwable)
				zipFile.closeWithException(throwable)
				throw throwable
			}
		}

		private fun openUsingZipInputStream(
			uri: Uri,
			zipEntryName: String,
			context: Context,
			signal: CancellationSignal?
		): ZipEntryStream? {
			val zipStream = ZipInputStream(context.contentResolver.openInputStream(uri))
			val zipEntry = try {
				zipStream.entries()
					.onEach { signal?.throwIfCanceled() }
					.firstOrNull { it.name == zipEntryName }
			} catch (throwable: Throwable) {
				zipStream.closeWithException(throwable)
				throw throwable
			}
			if (zipEntry == null) {
				zipStream.close()
				return null
			}
			return ZipEntryStream(zipStream, zipEntry.size)
		}
	}
}