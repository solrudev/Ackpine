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

package ru.solrudev.ackpine.splits

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import ru.solrudev.ackpine.helpers.closeWithException
import ru.solrudev.ackpine.helpers.getFileFromUri
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipFile

/**
 * Factories for [APK splits][Apk] [sequences][Sequence].
 */
public object ZippedApkSplits {

	/**
	 * Returns a lazy sequence of [APK splits][Apk] contained within zipped [file] (such as APKS, APKM, XAPK, ZIP).
	 * This sequence supports cancellation when used with [SplitPackage] API and [ApkSplits] transformations.
	 *
	 * **Attention:** iteration of this sequence is blocking due to I/O operations.
	 *
	 * The returned sequence is constrained to be iterated only once.
	 *
	 * @return [CloseableSequence]
	 */
	@JvmStatic
	public fun getApksForFile(file: File): CloseableSequence<Apk> = closeableSequence {
		yieldAllUsingFile(file)
	}

	/**
	 * Returns a lazy sequence of [APK splits][Apk] contained within zipped file (such as APKS, APKM, XAPK, ZIP) at
	 * provided [uri].
	 * This sequence supports cancellation when used with [SplitPackage] API and [ApkSplits] transformations.
	 *
	 * This function will call [Context.getApplicationContext] internally, so it's safe to pass in any Context.
	 *
	 * **Attention:** iteration of this sequence is blocking due to I/O operations.
	 *
	 * The returned sequence is constrained to be iterated only once.
	 *
	 * @return [CloseableSequence]
	 */
	@JvmStatic
	public fun getApksForUri(uri: Uri, context: Context): CloseableSequence<Apk> {
		val applicationContext = context.applicationContext // avoid capturing context into closure
		return closeableSequence {
			val file = applicationContext.getFileFromUri(uri)
			if (file.canRead()) {
				yieldAllUsingFile(file)
			} else {
				yieldAllUsingFileChannel(applicationContext, uri)
			}
		}
	}

	private suspend inline fun CloseableSequenceScope<Apk>.yieldAllUsingFile(file: File) {
		val zipFile = ZipFile(file).use()
		zipFile.entries()
			.asSequence()
			.filterNot { isClosed }
			.mapNotNull { zipEntry ->
				zipFile.getInputStream(zipEntry).use { entryStream ->
					// java.util.zip.ZipFile closes all entry streams when closed, no need to apply .use()
					Apk.fromZipEntry(file.absolutePath, zipEntry, entryStream, scope = this)
				}
			}
			.forEach { yield(it) }
	}

	private suspend inline fun CloseableSequenceScope<Apk>.yieldAllUsingFileChannel(context: Context, uri: Uri) {
		var fd: ParcelFileDescriptor? = null
		var fileInputStream: FileInputStream? = null
		val zipFile: ru.solrudev.ackpine.compress.archivers.zip.ZipFile
		try {
			fd = context.contentResolver.openFileDescriptor(uri, "r")?.use()
				?: throw NullPointerException("ParcelFileDescriptor was null: $uri")
			fileInputStream = FileInputStream(fd.fileDescriptor).use()
			zipFile = ru.solrudev.ackpine.compress.archivers.zip.ZipFile.builder()
				.setFileChannel(fileInputStream.channel)
				.get()
				.use()
		} catch (throwable: Throwable) {
			fd?.closeWithException(throwable)
			fileInputStream?.closeWithException(throwable)
			throw throwable
		}
		zipFile.entries
			.asSequence()
			.filterNot { isClosed }
			.mapNotNull { zipEntry ->
				zipFile.getInputStream(zipEntry).use { entryStream ->
					entryStream.use()
					Apk.fromZipEntry(uri.toString(), zipEntry, entryStream, scope = this)
				}
			}
			.forEach { yield(it) }
	}
}