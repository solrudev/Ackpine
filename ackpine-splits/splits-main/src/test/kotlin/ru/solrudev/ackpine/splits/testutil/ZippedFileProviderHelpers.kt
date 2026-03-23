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

import android.content.pm.ProviderInfo
import android.net.Uri
import org.robolectric.Robolectric
import ru.solrudev.ackpine.ZippedFileProvider
import java.io.File

const val ZIPPED_FILE_PROVIDER_AUTHORITY = "ru.solrudev.ackpine.ZippedFileProvider"

fun ZippedFileProvider.Companion.setup(): ZippedFileProvider {
	return Robolectric.buildContentProvider(ZippedFileProvider::class.java)
		.create(ProviderInfo().apply { authority = ZIPPED_FILE_PROVIDER_AUTHORITY })
		.get()
}

fun ZippedFileProvider.Companion.legacyUri(archive: File, entryName: String): Uri = Uri.Builder()
	.scheme("content")
	.authority(ZIPPED_FILE_PROVIDER_AUTHORITY)
	.encodedPath(Uri.fromFile(archive).toString())
	.encodedQuery(entryName)
	.build()