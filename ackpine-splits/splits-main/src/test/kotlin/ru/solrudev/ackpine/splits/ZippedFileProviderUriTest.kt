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

package ru.solrudev.ackpine.splits

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import ru.solrudev.ackpine.ZippedFileProvider
import ru.solrudev.ackpine.splits.testutil.SplitFixtures
import ru.solrudev.ackpine.splits.testutil.TestFileProvider
import ru.solrudev.ackpine.splits.testutil.ZIPPED_FILE_PROVIDER_AUTHORITY
import ru.solrudev.ackpine.splits.testutil.legacyUri
import ru.solrudev.ackpine.splits.testutil.setup
import java.io.FileNotFoundException
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class ZippedFileProviderUriTest {

	private val context: Context = ApplicationProvider.getApplicationContext()

	@BeforeTest
	fun setUp() {
		ZippedFileProvider.setup()
		TestFileProvider.setup()
	}

	@Test
	fun getUriForZipEntryCreatesValidUri() {
		val uri = ZippedFileProvider.getUriForZipEntry(Uri.fromFile(SplitFixtures.apksArchive()), "base-master.apk")
		assertEquals(ContentResolver.SCHEME_CONTENT, uri.scheme)
		assertEquals(ZIPPED_FILE_PROVIDER_AUTHORITY, uri.authority)
	}

	@Test
	fun getUriForZipEntryFromFileCreatesValidUri() {
		val uri = ZippedFileProvider.getUriForZipEntry(SplitFixtures.apksArchive(), "base-master.apk")
		assertEquals(ContentResolver.SCHEME_CONTENT, uri.scheme)
		assertEquals(ZIPPED_FILE_PROVIDER_AUTHORITY, uri.authority)
	}

	@Test
	fun getUriForZipEntryFromStringCreatesValidUri() {
		val uri = ZippedFileProvider.getUriForZipEntry("/path/to/archive.apks", "entry.apk")
		assertEquals(ContentResolver.SCHEME_CONTENT, uri.scheme)
		assertEquals(ZIPPED_FILE_PROVIDER_AUTHORITY, uri.authority)
	}

	@Test
	fun getUriForZipEntryFromStringPreservesRawPathSource() {
		val uri = ZippedFileProvider.getUriForZipEntry("/path/to/archive.apks", "entry.apk")
		assertEquals("/path/to/archive.apks", uri.getQueryParameter("source"))
		assertEquals("entry.apk", uri.getQueryParameter("entry"))
	}

	@Test
	fun getUriForZipEntryFromContentUriPreservesSource() {
		val archiveUri = TestFileProvider.getUri(SplitFixtures.apksArchive())
		val entryName = SplitFixtures.firstApkEntryMetadata().name
		val uri = ZippedFileProvider.getUriForZipEntry(archiveUri, entryName)
		assertEquals(archiveUri.toString(), uri.getQueryParameter("source"))
		assertEquals(entryName, uri.getQueryParameter("entry"))
	}

	@Test
	fun getUriForZipEntryPreservesEncodedPathSource() {
		val uri = ZippedFileProvider.getUriForZipEntry("/path/with spaces/archive.apks", "entry.apk")
		assertEquals("/path/with spaces/archive.apks", uri.getQueryParameter("source"))
		assertEquals("entry.apk", uri.getQueryParameter("entry"))
	}

	@Test
	fun getUriForZipEntryPreservesNestedEntryName() {
		val uri = ZippedFileProvider.getUriForZipEntry("/path/to/archive.apks", "splits/base-master.apk")
		assertEquals("splits/base-master.apk", uri.getQueryParameter("entry"))
	}

	@Test
	fun getTypeReturnsApkMimeTypeForAllSourceVariants() {
		val archive = SplitFixtures.apksArchive()
		val contentSource = TestFileProvider.getUri(archive)
		listOf(
			"file" to ZippedFileProvider.getUriForZipEntry(Uri.fromFile(archive), "base-master.apk"),
			"raw path" to Uri.parse(
				"content://$ZIPPED_FILE_PROVIDER_AUTHORITY/v2" +
						"?source=${Uri.encode("/plain/archive.apks")}" +
						"&entry=${Uri.encode("splits/base-master.apk")}"
			),
			"content" to ZippedFileProvider.getUriForZipEntry(contentSource, "base-master.apk"),
			"legacy file" to ZippedFileProvider.legacyUri(archive, "splits/base-master.apk"),
			"legacy raw path" to legacyRawPathUri()
		).forEach { (label, uri) ->
			assertEquals("application/vnd.android.package-archive", context.contentResolver.getType(uri), label)
		}
	}

	@Test
	fun getTypeReturnsNullForEntryWithoutExtension() {
		val uri = ZippedFileProvider.getUriForZipEntry(Uri.fromFile(SplitFixtures.apksArchive()), "splits/base-master")
		assertNull(context.contentResolver.getType(uri))
	}

	@Test
	fun isZippedFileProviderUriReturnsTrueForValidUriVariants() {
		listOf(
			"v2" to ZippedFileProvider.getUriForZipEntry(Uri.fromFile(SplitFixtures.apksArchive()), "base-master.apk"),
			"legacy" to ZippedFileProvider.legacyUri(SplitFixtures.apksArchive(), "splits/base-master.apk"),
			"legacy raw path" to legacyRawPathUri()
		).forEach { (label, uri) ->
			assertTrue(ZippedFileProvider.isZippedFileProviderUri(uri), label)
		}
	}

	@Test
	fun isZippedFileProviderUriReturnsFalseForWrongAuthority() {
		val uri = Uri.parse("content://wrong.authority/v2?source=file:///a.apks&entry=b.apk")
		assertFalse(ZippedFileProvider.isZippedFileProviderUri(uri))
	}

	@Test
	fun isZippedFileProviderUriReturnsFalseForMissingQueryParameters() {
		val uri = Uri.parse("content://$ZIPPED_FILE_PROVIDER_AUTHORITY/v2")
		assertFalse(ZippedFileProvider.isZippedFileProviderUri(uri))
	}

	@Test
	fun getTypeWithMalformedV2UriMissingSourceThrows() {
		val uri = Uri.parse("content://$ZIPPED_FILE_PROVIDER_AUTHORITY/v2?entry=splits/base-master.apk")
		assertFailsWith<FileNotFoundException> {
			context.contentResolver.getType(uri)
		}
	}

	@Test
	fun getTypeWithMalformedV2UriMissingEntryThrows() {
		val archive = SplitFixtures.apksArchive()
		val uri = Uri.parse(
			"content://$ZIPPED_FILE_PROVIDER_AUTHORITY/v2?source=${Uri.encode(Uri.fromFile(archive).toString())}"
		)
		assertFailsWith<FileNotFoundException> {
			context.contentResolver.getType(uri)
		}
	}

	@Test
	fun getTypeWithMalformedV2UriMissingBothParamsThrows() {
		val uri = Uri.parse("content://$ZIPPED_FILE_PROVIDER_AUTHORITY/v2")
		assertFailsWith<FileNotFoundException> {
			context.contentResolver.getType(uri)
		}
	}

	private fun legacyRawPathUri() = Uri.Builder()
		.scheme("content")
		.authority(ZIPPED_FILE_PROVIDER_AUTHORITY)
		.encodedPath("/plain/archive.apks")
		.encodedQuery("splits/base-master.apk")
		.build()
}