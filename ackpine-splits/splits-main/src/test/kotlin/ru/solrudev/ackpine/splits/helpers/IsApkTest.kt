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

package ru.solrudev.ackpine.splits.helpers

import java.util.zip.ZipEntry
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IsApkTest {

	@Test
	fun zipEntryIsApkReturnsTrueForApkFile() {
		val entry = ZipEntry("base.apk")
		assertTrue(entry.isApk)
	}

	@Test
	fun zipEntryIsApkReturnsTrueForMixedCaseExtension() {
		val entry = ZipEntry("base.ApK")
		assertTrue(entry.isApk)
	}

	@Test
	fun zipEntryIsApkReturnsFalseForNonApkFile() {
		val entry = ZipEntry("resources.arsc")
		assertFalse(entry.isApk)
	}

	@Test
	fun zipEntryIsApkReturnsFalseForDirectory() {
		val entry = ZipEntry("base.apk/")
		assertFalse(entry.isApk)
	}

	@Test
	fun zipEntryIsApkReturnsTrueForNestedApk() {
		val entry = ZipEntry("splits/config.arm64_v8a.apk")
		assertTrue(entry.isApk)
	}
}