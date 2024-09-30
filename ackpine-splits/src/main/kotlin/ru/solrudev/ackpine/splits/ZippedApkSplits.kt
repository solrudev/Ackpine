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

package ru.solrudev.ackpine.splits

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import ru.solrudev.ackpine.helpers.entries
import ru.solrudev.ackpine.helpers.toFile
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

/**
 * Factories for [APK splits][Apk] [sequences][Sequence].
 */
public object ZippedApkSplits {

	/**
	 * Returns a lazy sequence of [APK splits][Apk] contained within zipped [file] (such as APKS, APKM, XAPK, ZIP).
	 *
	 * **Attention:** iteration of this sequence is blocking due to I/O operations.
	 *
	 * The returned sequence is constrained to be iterated only once.
	 */
	@JvmStatic
	public fun getApksForFile(file: File): Sequence<Apk> = closeableSequence {
		ZipFile(file).use { zipFile ->
			addCloseableResource(zipFile)
			zipFile.entries()
				.asSequence()
				.filterNot { isClosed }
				.mapNotNull { zipEntry ->
					zipFile.getInputStream(zipEntry).use { entryStream ->
						Apk.fromZipEntry(file.absolutePath, zipEntry, entryStream)
					}
				}
				.forEach { yield(it) }
		}
	}

	/**
	 * Returns a lazy sequence of [APK splits][Apk] contained within zipped file (such as APKS, APKM, XAPK, ZIP) at
	 * provided [uri].
	 *
	 * This function will call [Context.getApplicationContext] internally, so it's safe to pass in any Context.
	 *
	 * **Attention:** iteration of this sequence is blocking due to I/O operations.
	 *
	 * The returned sequence is constrained to be iterated only once.
	 */
	@JvmStatic
	public fun getApksForUri(uri: Uri, context: Context): Sequence<Apk> {
		val applicationContext = context.applicationContext // avoid capturing context into closure
		return closeableSequence {
			val file = uri.toFile(applicationContext)
			if (file.canRead()) {
				yieldAll(getApksForFile(file))
				return@closeableSequence
			}
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				try {
					yieldAllUsingFileChannel(applicationContext, uri)
				} catch (exception: Exception) {
					exception.printStackTrace()
					yieldAllUsingZipInputStream(applicationContext, uri)
				}
				return@closeableSequence
			}
			yieldAllUsingZipInputStream(applicationContext, uri)
		}
	}

	@RequiresApi(Build.VERSION_CODES.O)
	private suspend inline fun CloseableSequenceScope<Apk>.yieldAllUsingFileChannel(context: Context, uri: Uri) {
		context.contentResolver.openFileDescriptor(uri, "r").use { fd ->
			fd ?: throw NullPointerException("ParcelFileDescriptor was null: $uri")
			addCloseableResource(fd)
			FileInputStream(fd.fileDescriptor).use { fileInputStream ->
				addCloseableResource(fileInputStream)
				org.apache.commons.compress.archivers.zip.ZipFile.builder()
					.setSeekableByteChannel(fileInputStream.channel)
					.get()
					.use { zipFile ->
						addCloseableResource(zipFile)
						zipFile.entries
							.asSequence()
							.filterNot { isClosed }
							.mapNotNull { zipEntry ->
								zipFile.getInputStream(zipEntry).use { entryStream ->
									addCloseableResource(entryStream)
									Apk.fromZipEntry(uri.toString(), zipEntry, entryStream)
								}
							}
							.forEach { yield(it) }
					}
			}
		}
	}

	private suspend inline fun CloseableSequenceScope<Apk>.yieldAllUsingZipInputStream(context: Context, uri: Uri) {
		ZipInputStream(context.contentResolver.openInputStream(uri)).use { zipStream ->
			addCloseableResource(zipStream)
			zipStream.entries()
				.filterNot { isClosed }
				.mapNotNull { zipEntry -> Apk.fromZipEntry(uri.toString(), zipEntry, zipStream) }
				.forEach { yield(it) }
		}
	}
}