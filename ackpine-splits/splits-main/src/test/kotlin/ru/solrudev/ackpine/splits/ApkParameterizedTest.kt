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
import androidx.test.core.app.ApplicationProvider
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import ru.solrudev.ackpine.splits.testutil.APK_FIXTURE_CASES
import ru.solrudev.ackpine.splits.testutil.ApkFixtureCase
import ru.solrudev.ackpine.splits.testutil.TestFileProvider
import ru.solrudev.ackpine.splits.testutil.SplitFixtures
import ru.solrudev.ackpine.splits.testutil.assertFixturePackage
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotNull

@RunWith(ParameterizedRobolectricTestRunner::class)
class ApkParameterizedTest(
	private val case: ApkFixtureCase,
	private val sourceKind: ApkSourceKind
) {

	private val context: Context = ApplicationProvider.getApplicationContext()

	@BeforeTest
	fun setUp() {
		TestFileProvider.setup()
	}

	@Test
	fun fromUriCreatesCorrectApkTypes() {
		val uri = when (sourceKind) {
			ApkSourceKind.FILE -> SplitFixtures.apkFileUri(case.fileName)
			ApkSourceKind.CONTENT -> TestFileProvider.getUri(SplitFixtures.apkFile(case.fileName))
		}
		val label = "${sourceKind.label} ${case.label}"

		val apk = assertNotNull(Apk.fromUri(uri, context), label)
		assertFixturePackage(apk, label)
		case.expectation.assertAgainst(apk, label)
	}

	private companion object {

		@Suppress("Unused")
		@JvmStatic
		@ParameterizedRobolectricTestRunner.Parameters(name = "{index}: {1} {0}")
		fun parameters() = APK_FIXTURE_CASES.flatMap { case ->
			ApkSourceKind.entries.map { sourceKind ->
				arrayOf(case, sourceKind)
			}
		}
	}
}

enum class ApkSourceKind(val label: String) {
	FILE("file URI"),
	CONTENT("content URI")
}