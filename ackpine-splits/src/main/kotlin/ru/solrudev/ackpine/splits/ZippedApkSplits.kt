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
import ru.solrudev.ackpine.helpers.entries
import ru.solrudev.ackpine.helpers.toFile
import java.io.File
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
	public fun getApksForFile(file: File): Sequence<Apk> = sequence {
		ZipFile(file).use { zipFile ->
			zipFile.entries()
				.asSequence()
				.mapNotNull { zipEntry ->
					zipFile.getInputStream(zipEntry).use { entryStream ->
						Apk.fromZipEntry(file.absolutePath, zipEntry, entryStream)
					}
				}
				.forEach { yield(it) }
		}
	}.constrainOnce()

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
		return sequence {
			val file = uri.toFile(applicationContext)
			if (file.canRead()) {
				yieldAll(getApksForFile(file))
				return@sequence
			}
			// fall back to ZipInputStream if file is not readable directly
			ZipInputStream(applicationContext.contentResolver.openInputStream(uri)).use { zipStream ->
				zipStream.entries()
					.mapNotNull { zipEntry -> Apk.fromZipEntry(uri.toString(), zipEntry, zipStream) }
					.forEach { yield(it) }
			}
		}.constrainOnce()
	}
}