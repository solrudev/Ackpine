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

package ru.solrudev.ackpine.sample.install

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.solrudev.ackpine.ZippedFileProvider
import ru.solrudev.ackpine.splits.Apk
import java.util.zip.ZipInputStream

internal const val V4_SIGNATURE_EXTENSION = ".idsig"

/**
 * Resolves v4 signatures for the APKs being installed, keyed by [Apk.name]. Invoked from a coroutine, because
 * resolving may need to read the picked files.
 */
internal typealias V4SignatureProvider = suspend () -> Map<String, Uri>

internal val EmptyV4SignatureProvider: V4SignatureProvider = { emptyMap() }

/**
 * Reads names of the `.idsig` entries in the ZIP archive at [zipUri] and maps them to the [names][Apk.name] of the
 * APKs they belong to.
 *
 * The APKs themselves are read by `ZippedApkSplits`, and the URIs it creates for them can't be reproduced here
 * reliably. That's not a problem, because only the keys of `InstallParameters.v4Signatures` have to match the APK
 * URIs, and they are taken from the resolved APKs. A signature's own URI just has to be readable.
 */
internal suspend fun zipV4Signatures(zipUri: Uri, context: Context): Map<String, Uri> {
	val applicationContext = context.applicationContext
	return withContext(Dispatchers.IO) {
		val signatures = mutableMapOf<String, Uri>()
		applicationContext.contentResolver.openInputStream(zipUri)?.use { inputStream ->
			ZipInputStream(inputStream.buffered()).use { zipInputStream ->
				while (true) {
					val entry = zipInputStream.nextEntry ?: break
					if (entry.isDirectory || !entry.name.endsWith(V4_SIGNATURE_EXTENSION, ignoreCase = true)) {
						continue
					}
					signatures[apkName(entry.name.dropLast(V4_SIGNATURE_EXTENSION.length))] =
						ZippedFileProvider.getUriForZipEntry(zipUri, entry.name)
				}
			}
		}
		signatures
	}
}

/**
 * Maps the picked `.idsig` files to the [names][Apk.name] of the APKs they belong to.
 *
 * Signatures are paired with APKs by file name, i.e. `base.apk.idsig` belongs to `base.apk`. As a convenience, a
 * single signature picked alongside a single APK is paired with it regardless of its file name.
 */
internal fun pickedV4Signatures(apkFiles: List<PickedFile>, signatureFiles: List<PickedFile>): Map<String, Uri> {
	if (signatureFiles.isEmpty()) {
		return emptyMap()
	}
	if (apkFiles.size == 1 && signatureFiles.size == 1) {
		return mapOf(apkName(apkFiles[0].name) to signatureFiles[0].uri)
	}
	return signatureFiles.associate { file ->
		apkName(file.name.dropLast(V4_SIGNATURE_EXTENSION.length)) to file.uri
	}
}

/**
 * Returns the [Apk.name] for a file or ZIP entry name, which is its name without a path and an extension.
 */
private fun apkName(apkFileName: String) = apkFileName.substringAfterLast('/').substringBeforeLast('.')
