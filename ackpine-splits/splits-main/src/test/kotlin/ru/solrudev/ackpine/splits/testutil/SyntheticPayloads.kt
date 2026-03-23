/*
 * Copyright (C) 2026 Ilya Fomichev
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

package ru.solrudev.ackpine.splits.testutil

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object SyntheticPayloads {

	private val TRUNCATED_ZIP = byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0x00)

	fun truncatedZipBytes() = TRUNCATED_ZIP.copyOf()
	fun truncatedZipUri(name: String) = rawBytesUri(name, TRUNCATED_ZIP)

	fun rawBytesUri(
		name: String,
		bytes: ByteArray
	) = TestFileProvider.createSource(name) { outputStream ->
		outputStream.write(bytes)
	}

	fun emptyZipUri(name: String) = TestFileProvider.createSource(name) { outputStream ->
		outputStream.write(emptyZipBytes())
	}

	fun emptyZipBytes(): ByteArray {
		val outputStream = ByteArrayOutputStream()
		ZipOutputStream(outputStream).use { it.finish() }
		return outputStream.toByteArray()
	}

	fun zipBytesOf(entries: Map<String, ByteArray>): ByteArray {
		val outputStream = ByteArrayOutputStream()
		ZipOutputStream(outputStream).use { zipOutputStream ->
			for ((entryName, data) in entries) {
				zipOutputStream.putNextEntry(ZipEntry(entryName))
				zipOutputStream.write(data)
				zipOutputStream.closeEntry()
			}
		}
		return outputStream.toByteArray()
	}

	fun zipUriOf(
		name: String,
		entries: Map<String, ByteArray>
	) = TestFileProvider.createSource(name) { outputStream ->
		outputStream.write(zipBytesOf(entries))
	}
}