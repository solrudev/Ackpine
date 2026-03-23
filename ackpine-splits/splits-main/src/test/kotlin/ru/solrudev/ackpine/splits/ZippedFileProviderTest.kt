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

import android.content.ContentValues
import android.content.Context
import android.content.res.AssetFileDescriptor.UNKNOWN_LENGTH
import android.net.Uri
import android.os.CancellationSignal
import android.os.OperationCanceledException
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
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
import java.io.IOException
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class ZippedFileProviderTest {

	private val context: Context = ApplicationProvider.getApplicationContext()
	private lateinit var provider: ZippedFileProvider

	@BeforeTest
	fun setUp() {
		provider = ZippedFileProvider.setup()
		TestFileProvider.setup()
	}

	@Test
	fun openFileReturnsExactEntryBytesForAllSourceVariants() {
		val entry = SplitFixtures.firstApkEntry()
		for (variant in firstEntryUriVariants(entry.metadata.name)) {
			provider.openFile(variant.uri, "r").use { descriptor ->
				ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { stream ->
					assertContentEquals(entry.bytes, stream.readBytes(), variant.label)
				}
			}
		}
	}

	@Test
	fun openAssetFileReturnsExactEntryBytesForAllSourceVariants() {
		val entry = SplitFixtures.firstApkEntry()
		for (variant in firstEntryUriVariants(entry.metadata.name)) {
			provider.openAssetFile(variant.uri, "r").use { descriptor ->
				descriptor.createInputStream().use { stream ->
					assertContentEquals(entry.bytes, stream.readBytes(), variant.label)
				}
			}
		}
	}

	@Test
	fun openAssetFileReturnsUnboundedDescriptorStartingAtZeroOffset() {
		val entryName = SplitFixtures.firstApkEntryMetadata().name
		provider.openAssetFile(firstEntryFileUri(entryName), "r").use { afd ->
			assertEquals(0L, afd.startOffset)
			assertEquals(UNKNOWN_LENGTH, afd.declaredLength)
		}
	}

	@Test
	fun openTypedAssetFileReturnsDescriptorForMatchingMimeTypes() {
		val entryName = SplitFixtures.firstApkEntryMetadata().name
		for (mimeType in listOf("*/*", "application/vnd.android.package-archive")) {
			provider.openTypedAssetFile(firstEntryFileUri(entryName), mimeType, null).use { descriptor ->
				assertEquals(0L, descriptor.startOffset, mimeType)
			}
		}
	}

	@Test
	fun openTypedAssetFileWithMismatchedMimeTypeThrows() {
		val entryName = SplitFixtures.firstApkEntryMetadata().name
		assertFailsWith<FileNotFoundException> {
			context.contentResolver
				.openTypedAssetFileDescriptor(firstEntryFileUri(entryName), "text/plain", null)
				?.close()
		}
	}

	@Test
	fun queryReturnsDisplayNameAndExactSizeForAllSourceVariants() {
		val (entryName, expectedSize) = SplitFixtures.firstApkEntryMetadata()
		for (variant in firstEntryUriVariants(entryName)) {
			context.contentResolver.query(
				variant.uri,
				arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
				null, null
			).use { cursor ->
				assertNotNull(cursor, variant.label)
				assertTrue(cursor.moveToFirst(), variant.label)
				assertEquals(entryName, cursor.getString(0), variant.label)
				assertEquals(expectedSize, cursor.getLong(1), variant.label)
			}
		}
	}

	@Test
	fun queryWithOnlyDisplayNameReturnsOnlyDisplayNameColumn() {
		val entryName = SplitFixtures.firstApkEntryMetadata().name
		context.contentResolver.query(
			firstEntryFileUri(entryName),
			arrayOf(OpenableColumns.DISPLAY_NAME),
			null, null
		).use { cursor ->
			assertNotNull(cursor)
			assertTrue(cursor.moveToFirst())
			assertEquals(entryName, cursor.getString(0))
			assertEquals(1, cursor.columnCount)
		}
	}

	@Test
	fun queryWithEmptyProjectionReturnsEmptyCursor() {
		val entryName = SplitFixtures.firstApkEntryMetadata().name
		context.contentResolver.query(firstEntryFileUri(entryName), emptyArray(), null, null).use { cursor ->
			assertNotNull(cursor)
			assertEquals(0, cursor.columnCount)
		}
	}

	@Test
	fun queryWithOnlySizeReturnsExactSize() {
		val (entryName, expectedSize) = SplitFixtures.firstApkEntryMetadata()
		context.contentResolver.query(
			firstEntryFileUri(entryName),
			arrayOf(OpenableColumns.SIZE),
			null, null
		).use { cursor ->
			assertNotNull(cursor)
			assertTrue(cursor.moveToFirst())
			assertEquals(expectedSize, cursor.getLong(0))
		}
	}

	@Test
	fun queryWithLegacyUriMissingEntryThrows() {
		val uri = Uri.Builder()
			.scheme("content")
			.authority(ZIPPED_FILE_PROVIDER_AUTHORITY)
			.encodedPath(Uri.fromFile(SplitFixtures.apksArchive()).toString())
			.build()
		assertFailsWith<FileNotFoundException> {
			context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.close()
		}
	}

	@Test
	fun writeOperationsAreUnsupported() {
		val uri = ZippedFileProvider.getUriForZipEntry(Uri.fromFile(SplitFixtures.apksArchive()), "base-master.apk")
		assertFailsWith<UnsupportedOperationException> {
			context.contentResolver.openFileDescriptor(uri, "w")?.close()
		}
		assertFailsWith<UnsupportedOperationException> {
			context.contentResolver.insert(uri, ContentValues())
		}
		assertFailsWith<UnsupportedOperationException> {
			context.contentResolver.delete(uri, null, null)
		}
		assertFailsWith<UnsupportedOperationException> {
			context.contentResolver.update(uri, ContentValues(), null, null)
		}
	}

	@Test
	fun missingEntryThrowsIOException() {
		val uri = ZippedFileProvider.getUriForZipEntry(Uri.fromFile(SplitFixtures.apksArchive()), "nonexistent.apk")
		assertFailsWith<IOException> {
			context.contentResolver.openInputStream(uri)?.close()
		}
	}

	@Test
	fun openFileThrowsForPreCancelledSignal() {
		val entryName = SplitFixtures.firstApkEntryMetadata().name
		val signal = CancellationSignal()
		signal.cancel()
		assertFailsWith<OperationCanceledException> {
			provider.openFile(firstEntryFileUri(entryName), "r", signal).close()
		}
	}

	@Test
	fun openAssetFileThrowsForPreCancelledSignal() {
		val entryName = SplitFixtures.firstApkEntryMetadata().name
		val signal = CancellationSignal()
		signal.cancel()
		assertFailsWith<OperationCanceledException> {
			provider.openAssetFile(firstEntryFileUri(entryName), "r", signal).close()
		}
	}

	private fun firstEntryFileUri(entryName: String) = ZippedFileProvider.getUriForZipEntry(
		Uri.fromFile(SplitFixtures.apksArchive()),
		entryName
	)

	private fun firstEntryUriVariants(entryName: String) = listOf(
		EntryUriVariant(
			label = "file",
			uri = firstEntryFileUri(entryName)
		),
		EntryUriVariant(
			label = "content",
			uri = ZippedFileProvider.getUriForZipEntry(TestFileProvider.getUri(SplitFixtures.apksArchive()), entryName)
		),
		EntryUriVariant(
			label = "legacy",
			uri = ZippedFileProvider.legacyUri(SplitFixtures.apksArchive(), entryName)
		)
	)
}

private data class EntryUriVariant(
	val label: String,
	val uri: Uri
)