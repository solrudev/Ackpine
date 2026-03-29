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

package ru.solrudev.ackpine.helpers

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import androidx.test.core.app.ApplicationProvider
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import ru.solrudev.ackpine.splits.testutil.TestDocumentsProvider
import ru.solrudev.ackpine.splits.testutil.TestFileProvider
import java.io.File
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class UriHelpersTest {

	private val context: Context = ApplicationProvider.getApplicationContext()

	@BeforeTest
	fun setUp() {
		TestFileProvider.setup()
		TestDocumentsProvider.setup()
	}

	@Test
	fun fileSchemeUriReturnsFile() {
		val file = File.createTempFile("test-", ".apk")
		file.deleteOnExit()
		val uri = Uri.fromFile(file)
		val result = context.getFileFromUri(uri)
		assertEquals(file, result)
	}

	@Test
	fun missingContentUriReturnsInvalidFile() {
		val uri = TestFileProvider.getUri(File(UUID.randomUUID().toString()))
		val result = context.getFileFromUri(uri)
		assertEquals(File(""), result)
	}

	@Test
	fun primaryStorageDocumentUriReturnsFile() {
		val uri = DocumentsContract.buildDocumentUri(
			TestDocumentsProvider.AUTHORITY,
			"primary:Download/app.apk"
		)
		val result = context.getFileFromUri(uri)
		assertEquals(File("/storage/primary/Download/app.apk"), result)
	}

	@Test
	fun nonPrimaryStorageDocumentUriWithChildReturnsFile() {
		val uri = DocumentsContract.buildDocumentUri(
			TestDocumentsProvider.AUTHORITY,
			"0123-4567:Music/app.apk"
		)
		val result = context.getFileFromUri(uri)
		val expected = File("${Environment.getExternalStorageDirectory().absolutePath}/Music/app.apk/")
		assertEquals(expected, result)
	}

	@Test
	fun nonPrimaryStorageDocumentRootReturnsExternalStorageDir() {
		val uri = DocumentsContract.buildDocumentUri(
			TestDocumentsProvider.AUTHORITY,
			"0123-4567"
		)
		val result = context.getFileFromUri(uri)
		assertEquals(Environment.getExternalStorageDirectory(), result)
	}

	@Test
	fun documentUriWithWrongAuthorityReturnsInvalidFile() {
		val uri = DocumentsContract.buildDocumentUri(
			"com.wrong.documents",
			"primary:Download/app.apk"
		)
		val result = context.getFileFromUri(uri)
		assertEquals(File(""), result)
	}

	@Test
	fun nonDocumentContentUriReturnsInvalidFile() {
		val uri = Uri.Builder()
			.scheme("content")
			.authority(TestDocumentsProvider.AUTHORITY)
			.appendPath("file")
			.appendPath("test")
			.build()
		val result = context.getFileFromUri(uri)
		assertEquals(File(""), result)
	}
}