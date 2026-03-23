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
import android.os.CancellationSignal
import android.os.OperationCanceledException
import androidx.test.core.app.ApplicationProvider
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import ru.solrudev.ackpine.splits.testutil.SplitFixtures
import ru.solrudev.ackpine.splits.testutil.SyntheticPayloads
import ru.solrudev.ackpine.splits.testutil.TestFileProvider
import java.io.IOException
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class ApkTest {

	private val context: Context = ApplicationProvider.getApplicationContext()

	@BeforeTest
	fun setUp() {
		TestFileProvider.setup()
	}

	@Test
	fun fromUriReturnsNullForNonApkFile() {
		val apk = Apk.fromUri(SplitFixtures.apkFileUri(SplitFixtures.APKS_ARCHIVE), context)
		assertNull(apk)
	}

	@Test
	fun fromUriThrowsForRawNonZipContentUri() {
		val uri = SyntheticPayloads.rawBytesUri("raw-not-zip.apk", "not a zip file".toByteArray())
		assertFailsWith<IOException> {
			Apk.fromUri(uri, context)
		}
	}

	@Test
	fun fromUriThrowsForTruncatedZipContentUri() {
		val uri = SyntheticPayloads.truncatedZipUri("truncated.apk")
		assertFailsWith<IOException> {
			Apk.fromUri(uri, context)
		}
	}

	@Test
	fun fromUriReturnsNullForEmptyValidZip() {
		val uri = SyntheticPayloads.emptyZipUri("empty-valid-zip.apk")
		assertNull(Apk.fromUri(uri, context))
	}

	@Test
	fun fromUriWithPreCancelledSignalThrows() {
		val contentUri = TestFileProvider.getUri(SplitFixtures.apkFile(SplitFixtures.BASE_APK))
		val signal = CancellationSignal()
		signal.cancel()
		assertFailsWith<OperationCanceledException> {
			Apk.fromUri(contentUri, context, signal)
		}
	}
}