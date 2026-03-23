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

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import ru.solrudev.ackpine.ZippedFileProvider
import ru.solrudev.ackpine.splits.testutil.APK_FIXTURE_CASES
import ru.solrudev.ackpine.splits.testutil.SplitFixtures
import ru.solrudev.ackpine.splits.testutil.SyntheticPayloads
import ru.solrudev.ackpine.splits.testutil.TestFileProvider
import ru.solrudev.ackpine.splits.testutil.assertFixturePackage
import ru.solrudev.ackpine.splits.testutil.setup
import java.io.File
import java.io.IOException
import java.util.zip.ZipException
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class ZippedApkSplitsTest {

	private val context: Context = ApplicationProvider.getApplicationContext()

	@BeforeTest
	fun setUp() {
		ZippedFileProvider.setup()
		TestFileProvider.setup()
	}

	@Test
	fun allSourceVariantsReturnExactSplitSet() {
		for (variant in apkSources()) {
			assertEquals(EXPECTED_APKS, variant.apks.mapTo(mutableSetOf(), Apk::stripped), variant.label)
		}
	}

	@Test
	fun allSourceVariantsContainExpectedFixtureCases() {
		for (variant in apkSources()) {
			for (case in APK_FIXTURE_CASES) {
				val apk = parsedStrippedApk(case.fileName)
				val apks = variant.apks.map { it.stripped() }
				assertContains(apks, apk)
				assertFixturePackage(apk, "${variant.label}: ${case.label}")
				case.expectation.assertAgainst(apk, "${variant.label}: ${case.label}")
			}
		}
	}

	@Test
	fun allSourceVariantsHaveZippedFileProviderUris() {
		for (variant in apkSources()) {
			for (apk in variant.apks) {
				assertTrue(ZippedFileProvider.isZippedFileProviderUri(apk.uri), variant.label)
			}
		}
	}

	@Test
	fun getApksForFileThrowsForNonZipContent() {
		val notZip = tempFile("not a zip file".toByteArray())
		assertFailsWith<ZipException> {
			ZippedApkSplits.getApksForFile(notZip).use { it.toList() }
		}
	}

	@Test
	fun getApksForFileThrowsForTruncatedZip() {
		assertFailsWith<IOException> {
			getApksForFile(SyntheticPayloads.truncatedZipBytes())
		}
	}

	@Test
	fun getApksForFileReturnsEmptyForEmptyValidZip() {
		assertTrue(getApksForFile(SyntheticPayloads.emptyZipBytes()).isEmpty())
	}

	@Test
	fun getApksForFileSkipsEmptyValidZipEntry() {
		val apks = getApksForFile(
			SyntheticPayloads.zipBytesOf(entries = mapOf("empty.apk" to SyntheticPayloads.emptyZipBytes()))
		)
		assertTrue(apks.isEmpty())
	}

	@Test
	fun getApksForFileSkipsMalformedApkEntryAndReturnsValidBaseApk() {
		val validApkBytes = SplitFixtures.apkFile(SplitFixtures.BASE_APK).readBytes()
		val apks = getApksForFile(
			SyntheticPayloads.zipBytesOf(
				entries = mapOf(
					"valid.apk" to validApkBytes,
					"malformed.apk" to "not a zip file".toByteArray()
				)
			)
		)
		assertContainsOnlyValidBaseApk(apks, "valid")
	}

	@Test
	fun getApksForUriThrowsForNonZipContent() {
		val uri = SyntheticPayloads.rawBytesUri("raw-not-zip.apks", "not a zip file".toByteArray())
		assertFailsWith<IOException> {
			ZippedApkSplits.getApksForUri(uri, context).use { it.toList() }
		}
	}

	@Test
	fun getApksForUriThrowsForTruncatedZip() {
		val uri = SyntheticPayloads.truncatedZipUri("truncated.apks")
		assertFailsWith<IOException> {
			ZippedApkSplits.getApksForUri(uri, context).use { it.toList() }
		}
	}

	@Test
	fun getApksForUriReturnsEmptyForEmptyValidZip() {
		val uri = SyntheticPayloads.emptyZipUri("empty-valid-zip.apks")
		val apks = ZippedApkSplits.getApksForUri(uri, context).use { it.toList() }
		assertTrue(apks.isEmpty())
	}

	@Test
	fun getApksForUriSkipsEmptyValidZipEntry() {
		val uri = SyntheticPayloads.zipUriOf(
			name = "apks-with-empty-valid-zip-entry.apks",
			entries = mapOf("empty.apk" to SyntheticPayloads.emptyZipBytes())
		)
		val apks = ZippedApkSplits.getApksForUri(uri, context).use { it.toList() }
		assertTrue(apks.isEmpty())
	}

	@Test
	fun getApksForUriSkipsMalformedApkEntryAndReturnsValidBaseApk() {
		val validApkBytes = SplitFixtures.apkFile(SplitFixtures.BASE_APK).readBytes()
		val uri = SyntheticPayloads.zipUriOf(
			name = "apks-with-malformed-entry.apks",
			entries = mapOf(
				"valid.apk" to validApkBytes,
				"malformed.apk" to "not a zip file".toByteArray()
			)
		)
		val apks = ZippedApkSplits.getApksForUri(uri, context).use { it.toList() }
		assertContainsOnlyValidBaseApk(apks, "valid")
	}

	private fun getApksForFile() = ZippedApkSplits.getApksForFile(SplitFixtures.apksArchive()).use { it.toList() }

	private fun getApksForFile(bytes: ByteArray): List<Apk> {
		val archive = tempFile(bytes)
		return ZippedApkSplits.getApksForFile(archive).use { it.toList() }
	}

	private fun tempFile(bytes: ByteArray) = File.createTempFile("synthetic", ".apks").apply {
		deleteOnExit()
		writeBytes(bytes)
	}

	private fun assertContainsOnlyValidBaseApk(apks: List<Apk>, expectedName: String) {
		assertEquals(1, apks.size, "Malformed entry should be skipped")
		val apk = assertIs<Apk.Base>(apks.single())
		assertFixturePackage(apk)
		assertEquals(expectedName, apk.name)
	}

	private fun apkSources() = listOf(
		ApkSourceVariant(
			label = "file",
			apks = getApksForFile()
		),
		ApkSourceVariant(
			label = "file URI",
			apks = ZippedApkSplits
				.getApksForUri(Uri.fromFile(SplitFixtures.apksArchive()), context)
				.use { it.toList() }
		),
		ApkSourceVariant(
			label = "content URI",
			apks = ZippedApkSplits
				.getApksForUri(TestFileProvider.getUri(SplitFixtures.apksArchive()), context)
				.use { it.toList() }
		)
	)

	private class ApkSourceVariant(
		val label: String,
		val apks: List<Apk>
	)
}

private fun Apk.stripped() = when (this) {
	is Apk.Base -> copy(uri = Uri.EMPTY, size = 0L)
	is Apk.Feature -> copy(uri = Uri.EMPTY, size = 0L)
	is Apk.Libs -> copy(uri = Uri.EMPTY, size = 0L)
	is Apk.ScreenDensity -> copy(uri = Uri.EMPTY, size = 0L)
	is Apk.Localization -> copy(uri = Uri.EMPTY, size = 0L)
	is Apk.Other -> copy(uri = Uri.EMPTY, size = 0L)
}

private fun parsedStrippedApk(fileName: String) = assertNotNull(
	Apk.fromUri(
		SplitFixtures.apkFileUri(fileName),
		context = ApplicationProvider.getApplicationContext()
	),
	"Expected fixture APK '$fileName' to parse successfully"
).stripped()

private val EXPECTED_ARCHIVE_APK_ENTRIES = setOf(
	"asset-slices/${SplitFixtures.ASSET_PACK_ASTC_APK}",
	"asset-slices/${SplitFixtures.ASSET_PACK_MASTER_APK}",
	"asset-slices/${SplitFixtures.ASSET_PACK_FALLBACK_APK}",
	"splits/${SplitFixtures.ARM64_V8A_APK}",
	"splits/${SplitFixtures.ARMEABI_V7A_APK}",
	"splits/${SplitFixtures.LOCALE_APK}",
	"splits/${SplitFixtures.HDPI_APK}",
	"splits/${SplitFixtures.LDPI_APK}",
	"splits/${SplitFixtures.BASE_APK}",
	"splits/${SplitFixtures.MDPI_APK}",
	"splits/${SplitFixtures.TVDPI_APK}",
	"splits/${SplitFixtures.XHDPI_APK}",
	"splits/${SplitFixtures.XXHDPI_APK}",
	"splits/${SplitFixtures.XXXHDPI_APK}",
	"splits/${SplitFixtures.FEATURE_LOCALE_APK}",
	"splits/${SplitFixtures.FEATURE_HDPI_APK}",
	"splits/${SplitFixtures.FEATURE_LDPI_APK}",
	"splits/${SplitFixtures.FEATURE_APK}",
	"splits/${SplitFixtures.FEATURE_MDPI_APK}",
	"splits/${SplitFixtures.FEATURE_TVDPI_APK}",
	"splits/${SplitFixtures.FEATURE_XHDPI_APK}",
	"splits/${SplitFixtures.FEATURE_XXHDPI_APK}",
	"splits/${SplitFixtures.FEATURE_XXXHDPI_APK}"
)

private val EXPECTED_APKS: Set<Apk> by lazy(LazyThreadSafetyMode.NONE) {
	EXPECTED_ARCHIVE_APK_ENTRIES.mapTo(mutableSetOf()) { entryName ->
		parsedStrippedApk(entryName.substringAfterLast('/'))
	}
}