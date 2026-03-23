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

package ru.solrudev.ackpine.splits.parsing

import com.android.apksig.internal.apk.AndroidBinXmlParser
import ru.solrudev.ackpine.exceptions.InvalidManifestAttributeException
import java.nio.ByteBuffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"

class AndroidManifestTest {

	@Test
	fun splitNameReturnsValueFromManifest() {
		val manifest = AndroidManifest(mapOf("split" to "config.hdpi"), "test")
		assertEquals("config.hdpi", manifest.splitName)
	}

	@Test
	fun splitNameReturnsEmptyWhenAbsent() {
		val manifest = AndroidManifest(emptyMap(), "test")
		assertEquals("", manifest.splitName)
	}

	@Test
	fun packageNameReturnsValueFromManifest() {
		val manifest = AndroidManifest(mapOf("package" to "com.example.app"), "test")
		assertEquals("com.example.app", manifest.packageName)
	}

	@Test
	fun packageNameThrowsWhenMissing() {
		val manifest = AndroidManifest(emptyMap(), "test")
		val exception = assertFailsWith<InvalidManifestAttributeException> {
			manifest.packageName
		}
		assertEquals("package", exception.attribute)
		assertEquals("test", exception.apkName)
	}

	@Test
	fun versionCodeReturnsValueFromManifest() {
		val manifest = AndroidManifest(
			mapOf("$ANDROID_NAMESPACE:versionCode" to "42"),
			"test"
		)
		assertEquals(42L, manifest.versionCode)
	}

	@Test
	fun versionCodeThrowsWhenMissing() {
		val manifest = AndroidManifest(emptyMap(), "test")
		assertFailsWith<InvalidManifestAttributeException> {
			manifest.versionCode
		}
	}

	@Test
	fun versionCodeThrowsWhenNotNumeric() {
		val manifest = AndroidManifest(
			mapOf("$ANDROID_NAMESPACE:versionCode" to "abc"),
			"test"
		)
		assertFailsWith<InvalidManifestAttributeException> {
			manifest.versionCode
		}
	}

	@Test
	fun versionCodeCombinesWithVersionCodeMajor() {
		val manifest = AndroidManifest(
			mapOf(
				"$ANDROID_NAMESPACE:versionCode" to "1",
				"$ANDROID_NAMESPACE:versionCodeMajor" to "2"
			),
			"test"
		)
		val expected = (2L shl 32) or 1L
		assertEquals(expected, manifest.versionCode)
	}

	@Test
	fun versionCodeWithZeroMajor() {
		val manifest = AndroidManifest(
			mapOf(
				"$ANDROID_NAMESPACE:versionCode" to "100",
				"$ANDROID_NAMESPACE:versionCodeMajor" to "0"
			),
			"test"
		)
		assertEquals(100L, manifest.versionCode)
	}

	@Test
	fun versionCodeMajorThrowsWhenNotNumeric() {
		val manifest = AndroidManifest(
			mapOf(
				"$ANDROID_NAMESPACE:versionCode" to "1",
				"$ANDROID_NAMESPACE:versionCodeMajor" to "abc"
			),
			"test"
		)
		assertFailsWith<InvalidManifestAttributeException> {
			manifest.versionCode
		}
	}

	@Test
	fun versionNameReturnsValueFromManifest() {
		val manifest = AndroidManifest(
			mapOf("$ANDROID_NAMESPACE:versionName" to "1.5.6"),
			"test"
		)
		assertEquals("1.5.6", manifest.versionName)
	}

	@Test
	fun versionNameReturnsEmptyWhenAbsent() {
		val manifest = AndroidManifest(emptyMap(), "test")
		assertEquals("", manifest.versionName)
	}

	@Test
	fun isFeatureSplitReturnsValueFromManifest() {
		val featureSplit = AndroidManifest(
			mapOf("$ANDROID_NAMESPACE:isFeatureSplit" to "true"),
			"test"
		)
		val notFeatureSplit = AndroidManifest(
			mapOf("$ANDROID_NAMESPACE:isFeatureSplit" to "false"),
			"test"
		)
		assertTrue(featureSplit.isFeatureSplit)
		assertFalse(notFeatureSplit.isFeatureSplit)
	}

	@Test
	fun isFeatureSplitReturnsFalseWhenAbsent() {
		val manifest = AndroidManifest(emptyMap(), "test")
		assertFalse(manifest.isFeatureSplit)
	}

	@Test
	fun configForSplitReturnsValueFromManifest() {
		val manifest = AndroidManifest(mapOf("configForSplit" to "base"), "test")
		assertEquals("base", manifest.configForSplit)
	}

	@Test
	fun configForSplitReturnsEmptyWhenAbsent() {
		val manifest = AndroidManifest(emptyMap(), "test")
		assertEquals("", manifest.configForSplit)
	}

	@Test
	fun emptyByteBufferThrowsXmlParserException() {
		val empty = ByteBuffer.allocate(0)
		assertFailsWith<AndroidBinXmlParser.XmlParserException> {
			AndroidManifest(empty, "empty")
		}
	}

	@Test
	fun invalidDataThrowsXmlParserException() {
		val invalid = ByteBuffer.wrap(byteArrayOf(0, 1, 2, 3))
		assertFailsWith<AndroidBinXmlParser.XmlParserException> {
			AndroidManifest(invalid, "invalid")
		}
	}
}