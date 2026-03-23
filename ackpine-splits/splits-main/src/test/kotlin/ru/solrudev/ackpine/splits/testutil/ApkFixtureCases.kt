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

import ru.solrudev.ackpine.splits.Abi
import ru.solrudev.ackpine.splits.Apk
import ru.solrudev.ackpine.splits.Dpi
import kotlin.test.assertEquals
import kotlin.test.assertIs

val APK_FIXTURE_CASES = listOf(
	ApkFixtureCase(
		fileName = SplitFixtures.BASE_APK,
		label = "base APK",
		expectation = BaseApkExpectation()
	),
	ApkFixtureCase(
		fileName = SplitFixtures.FEATURE_APK,
		label = "feature APK",
		expectation = FeatureApkExpectation(SplitFixtures.FEATURE_NAME)
	),
	ApkFixtureCase(
		fileName = SplitFixtures.XXHDPI_APK,
		label = "base density split",
		expectation = ScreenDensityApkExpectation(
			splitName = "config.xxhdpi",
			dpi = Dpi.XXHDPI
		)
	),
	ApkFixtureCase(
		fileName = SplitFixtures.ARM64_V8A_APK,
		label = "base ABI split",
		expectation = LibsApkExpectation(
			splitName = "config.arm64_v8a",
			abi = Abi.ARM64_V8A
		)
	),
	ApkFixtureCase(
		fileName = SplitFixtures.LOCALE_APK,
		label = "base localization split",
		expectation = LocalizationApkExpectation(
			splitName = "config.en",
			language = "en"
		)
	),
	ApkFixtureCase(
		fileName = SplitFixtures.FEATURE_XXHDPI_APK,
		label = "feature-targeted config split",
		expectation = ScreenDensityApkExpectation(
			splitName = "${SplitFixtures.FEATURE_NAME}.config.xxhdpi",
			configForSplit = SplitFixtures.FEATURE_NAME,
			dpi = Dpi.XXHDPI
		)
	),
	ApkFixtureCase(
		fileName = SplitFixtures.ASSET_PACK_ASTC_APK,
		label = "asset-pack ASTC split",
		expectation = OtherApkExpectation(
			splitName = "${SplitFixtures.ASSET_PACK_NAME}.config.astc",
			configForSplit = SplitFixtures.ASSET_PACK_NAME
		)
	),
	ApkFixtureCase(
		fileName = SplitFixtures.ASSET_PACK_FALLBACK_APK,
		label = "asset-pack fallback split",
		expectation = OtherApkExpectation(
			splitName = "${SplitFixtures.ASSET_PACK_NAME}.config.other_tcf",
			configForSplit = SplitFixtures.ASSET_PACK_NAME
		)
	)
)

data class ApkFixtureCase(
	val fileName: String,
	val label: String,
	val expectation: ApkFixtureExpectation
) {
	override fun toString() = label
}

sealed interface ApkFixtureExpectation {
	val splitName: String
	val isFeatureSplit: Boolean
	val configForSplit: String
	fun assertAgainst(apk: Apk, label: String)
}

class BaseApkExpectation(
	override val splitName: String = "",
	private val versionName: String = SplitFixtures.VERSION_NAME
) : ApkFixtureExpectation {

	override val isFeatureSplit = false
	override val configForSplit = ""

	override fun assertAgainst(apk: Apk, label: String) {
		val base = assertIs<Apk.Base>(apk, "$label: expected base APK")
		assertEquals(versionName, base.versionName, "$label: versionName")
	}
}

class FeatureApkExpectation(private val featureName: String) : ApkFixtureExpectation {

	override val splitName = featureName
	override val isFeatureSplit = true
	override val configForSplit = ""

	override fun assertAgainst(apk: Apk, label: String) {
		val feature = assertIs<Apk.Feature>(apk, "$label: expected feature APK")
		assertEquals(featureName, feature.name, "$label: feature name")
	}
}

class ScreenDensityApkExpectation(
	override val splitName: String,
	override val configForSplit: String = "",
	private val dpi: Dpi
) : ApkFixtureExpectation {

	override val isFeatureSplit = false

	override fun assertAgainst(apk: Apk, label: String) {
		val density = assertIs<Apk.ScreenDensity>(apk, "$label: expected screen-density APK")
		assertEquals(dpi, density.dpi, "$label: dpi")
		assertEquals(configForSplit, density.configForSplit, "$label: configForSplit")
	}
}

class LibsApkExpectation(
	override val splitName: String,
	override val configForSplit: String = "",
	private val abi: Abi
) : ApkFixtureExpectation {

	override val isFeatureSplit = false

	override fun assertAgainst(apk: Apk, label: String) {
		val libs = assertIs<Apk.Libs>(apk, "$label: expected libs APK")
		assertEquals(abi, libs.abi, "$label: abi")
		assertEquals(configForSplit, libs.configForSplit, "$label: configForSplit")
	}
}

class LocalizationApkExpectation(
	override val splitName: String,
	override val configForSplit: String = "",
	private val language: String
) : ApkFixtureExpectation {

	override val isFeatureSplit = false

	override fun assertAgainst(apk: Apk, label: String) {
		val localization = assertIs<Apk.Localization>(apk, "$label: expected localization APK")
		assertEquals(language, localization.locale.language, "$label: locale")
		assertEquals(configForSplit, localization.configForSplit, "$label: configForSplit")
	}
}

class OtherApkExpectation(
	override val splitName: String,
	override val configForSplit: String = ""
) : ApkFixtureExpectation {

	override val isFeatureSplit = false

	override fun assertAgainst(apk: Apk, label: String) {
		val other = assertIs<Apk.Other>(apk, "$label: expected other APK")
		assertEquals(configForSplit, other.configForSplit, "$label: configForSplit")
	}
}

fun assertFixturePackage(apk: Apk, message: String? = null) {
	assertEquals(SplitFixtures.PACKAGE_NAME, apk.packageName, "$message - packageName")
	assertEquals(SplitFixtures.VERSION_CODE, apk.versionCode, "$message - versionCode")
}