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
import androidx.annotation.RestrictTo
import ru.solrudev.ackpine.helpers.closeAll
import ru.solrudev.ackpine.helpers.closeAllWithException
import ru.solrudev.ackpine.helpers.closeWithException
import ru.solrudev.ackpine.helpers.getFileFromUri
import java.io.EOFException
import java.io.File
import java.io.FileInputStream
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipException
import java.util.zip.ZipFile

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
			val zipFile = try {
				ZipFile(file)
			} catch (exception: ZipException) {
				throw ZipEntryStreamException(exception)
			}
			try {
				val zipEntry = zipFile.getEntry(zipEntryName)
				if (zipEntry == null) {
					zipFile.close()
					return null
				}
				return ZipEntryStream(zipFile.getInputStream(zipEntry), zipEntry.size, zipFile)
			} catch (exception: ZipException) {
				closeAndThrow(ZipEntryStreamException(exception), zipFile)
			} catch (exception: EOFException) {
				closeAndThrow(ZipEntryStreamException(exception), zipFile)
			} catch (throwable: Throwable) {
				closeAndThrow(throwable, zipFile)
			}
		}

		private fun openUsingFileChannel(
			uri: Uri,
			zipEntryName: String,
			context: Context,
			signal: CancellationSignal?
		): ZipEntryStream? {
			val fd = context.contentResolver.openFileDescriptor(uri, "r", signal)
				?: throw NullPointerException("ParcelFileDescriptor was null: $uri")
			val fileInputStream = try {
				FileInputStream(fd.fileDescriptor)
			} catch (throwable: Throwable) {
				fd.closeWithException(throwable)
				throw throwable
			}
			val zipFile = wrapZipExceptions(fd, fileInputStream) {
				ru.solrudev.ackpine.compress.archivers.zip.ZipFile.builder()
					.setFileChannel(fileInputStream.channel)
					.get()
			}
			wrapZipExceptions(fd, fileInputStream, zipFile) {
				val zipEntry = zipFile.getEntry(zipEntryName)
				if (zipEntry == null) {
					closeAll(fd, fileInputStream, zipFile)
					return null
				}
				return ZipEntryStream(zipFile.getInputStream(zipEntry), zipEntry.size, zipFile, fileInputStream, fd)
			}
		}

		private inline fun <R> wrapZipExceptions(vararg resources: AutoCloseable, block: () -> R): R {
			try {
				return block()
			} catch (exception: ZipException) {
				closeAndThrow(ZipEntryStreamException(exception), resources.asIterable())
			} catch (exception: EOFException) {
				closeAndThrow(ZipEntryStreamException(exception), resources.asIterable())
			} catch (exception: IllegalArgumentException) {
				closeAndThrow(ZipEntryStreamException(exception), resources.asIterable())
			} catch (throwable: Throwable) {
				closeAndThrow(throwable, resources.asIterable())
			}
		}

		private fun closeAndThrow(throwable: Throwable, resources: Iterable<AutoCloseable>): Nothing {
			closeAllWithException(resources, throwable)
			throw throwable
		}

		private fun closeAndThrow(throwable: Throwable, resource: AutoCloseable): Nothing {
			resource.closeWithException(throwable)
			throw throwable
		}
	}
}

/**
 * Signals that an error related to corrupt or invalid ZIP occurred. It doesn't include I/O or permission errors.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class ZipEntryStreamException(cause: Throwable) : IOException(cause)