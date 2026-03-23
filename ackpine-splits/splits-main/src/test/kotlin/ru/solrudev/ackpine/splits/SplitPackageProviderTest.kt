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
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowBuild
import ru.solrudev.ackpine.helpers.ImmediateListenableFuture
import ru.solrudev.ackpine.splits.SplitPackage.Companion.toSplitPackage
import ru.solrudev.ackpine.splits.testutil.createBaseApk
import ru.solrudev.ackpine.splits.testutil.createFeatureApk
import ru.solrudev.ackpine.splits.testutil.createLibsApk
import ru.solrudev.ackpine.splits.testutil.createLocalizationApk
import ru.solrudev.ackpine.splits.testutil.createScreenDensityApk
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS-xxhdpi")
class SplitPackageProviderTest {

	private val context: Context = ApplicationProvider.getApplicationContext()

	@BeforeTest
	fun setUp() {
		ShadowBuild.setSupportedAbis(arrayOf("arm64-v8a", "armeabi-v7a"))
	}

	@AfterTest
	fun tearDown() {
		ShadowBuild.reset()
	}

	@Test
	fun emptyProviderSortedByCompatibilityReturnsSameProvider() {
		val empty = SplitPackage.empty()
		assertSame(empty, empty.sortedByCompatibility(context))
	}

	@Test
	fun emptyProviderFilterCompatibleReturnsSameProvider() {
		val empty = SplitPackage.empty()
		assertSame(empty, empty.filterCompatible(context))
	}

	@Test
	fun sortedByCompatibilityIsIdempotent() {
		val provider = createProvider()
		val sorted = provider.sortedByCompatibility(context)
		assertSame(sorted, sorted.sortedByCompatibility(context))
	}

	@Test
	fun filterCompatibleIsIdempotent() {
		val provider = createProvider()
		val filtered = provider.filterCompatible(context)
		assertSame(filtered, filtered.filterCompatible(context))
	}

	@Test
	fun sortedByCompatibilityOnFilteredIsNoOp() {
		val provider = createProvider()
		val filtered = provider.filterCompatible(context)
		assertSame(filtered, filtered.sortedByCompatibility(context))
	}

	@Test
	fun sortedByCompatibilityPutsClosestDensityFirstAndMarksAsPreferred() {
		val ldpi = createScreenDensityApk(name = "config.ldpi", dpi = Dpi.LDPI)
		val xxhdpi = createScreenDensityApk(name = "config.xxhdpi", dpi = Dpi.XXHDPI)
		val hdpi = createScreenDensityApk(name = "config.hdpi", dpi = Dpi.HDPI)
		val provider = listOf(ldpi, hdpi, xxhdpi, createBaseApk()).toSplitPackage()

		val sorted = provider.sortedByCompatibility(context).getAsync().get()

		assertEquals(Dpi.XXHDPI, sorted.screenDensity.first().apk.dpi)
		assertTrue(sorted.screenDensity.first().isPreferred)
		assertFalse(sorted.screenDensity.last().isPreferred)
	}

	@Test
	fun sortedByCompatibilitySkipsSortingForAlreadySortedSplitPackage() {
		val alreadySorted = createProvider().sortedByCompatibility(context).getAsync().get()
		val wrappingProvider = SplitPackage.Provider { ImmediateListenableFuture(alreadySorted) }

		val result = wrappingProvider.sortedByCompatibility(context).getAsync().get()

		assertSame(alreadySorted, result)
	}

	@Test
	fun sortedByCompatibilitySkipsSortingForAlreadyFilteredSplitPackage() {
		val alreadyFiltered = createProvider().filterCompatible(context).getAsync().get()
		val wrappingProvider = SplitPackage.Provider { ImmediateListenableFuture(alreadyFiltered) }

		val result = wrappingProvider.sortedByCompatibility(context).getAsync().get()

		assertSame(alreadyFiltered, result)
	}

	@Test
	fun sortedByCompatibilityPrefersHigherDensityWhenEquidistant() {
		val xhdpi = createScreenDensityApk(name = "config.xhdpi", dpi = Dpi.XHDPI)
		val xxxhdpi = createScreenDensityApk(name = "config.xxxhdpi", dpi = Dpi.XXXHDPI)
		val provider = listOf(xhdpi, xxxhdpi, createBaseApk()).toSplitPackage()

		val sorted = provider.sortedByCompatibility(context).getAsync().get()

		assertEquals(Dpi.XXXHDPI, sorted.screenDensity.first().apk.dpi)
	}

	@Test
	fun sortedByCompatibilityPutsPreferredAbiFirst() {
		val preferred = createLibsApk(name = "config.arm64_v8a", abi = Abi.ARM64_V8A)
		val other = createLibsApk(name = "config.x86", abi = Abi.X86)
		val provider = listOf(other, preferred, createBaseApk()).toSplitPackage()

		val sorted = provider.sortedByCompatibility(context).getAsync().get()

		assertEquals(Abi.ARM64_V8A, sorted.libs.first().apk.abi)
		assertTrue(sorted.libs.first().isPreferred)
	}

	@Test
	fun sortedByCompatibilityMarksIncompatibleAbiAsNotPreferred() {
		val libs = createLibsApk(name = "config.x86", abi = Abi.X86)
		val provider = listOf(libs, createBaseApk()).toSplitPackage()

		val sorted = provider.sortedByCompatibility(context).getAsync().get()

		assertFalse(sorted.libs.single().isPreferred)
	}

	@Test
	fun sortedByCompatibilityPutsMatchingLocaleFirst() {
		val japanese = createLocalizationApk(name = "config.ja", locale = Locale("ja"))
		val english = createLocalizationApk(name = "config.en", locale = Locale("en"))
		val provider = listOf(japanese, english, createBaseApk()).toSplitPackage()

		val sorted = provider.sortedByCompatibility(context).getAsync().get()

		assertEquals(Locale("en"), sorted.localization.first().apk.locale)
		assertTrue(sorted.localization.first().isPreferred)
	}

	@Test
	fun sortedByCompatibilityMarksNonMatchingLocaleAsNotPreferred() {
		val japanese = createLocalizationApk(name = "config.ja", locale = Locale("ja"))
		val provider = listOf(japanese, createBaseApk()).toSplitPackage()

		val sorted = provider.sortedByCompatibility(context).getAsync().get()

		assertFalse(sorted.localization.single().isPreferred)
	}

	@Test
	fun sortedByCompatibilityPrefersBaseTargetedOverFeatureTargeted() {
		val baseTargeted = createScreenDensityApk(name = "config.xxhdpi", dpi = Dpi.XXHDPI, configForSplit = "")
		val featureTargeted = createScreenDensityApk(
			name = "missing.config.xxhdpi",
			dpi = Dpi.XXHDPI,
			configForSplit = "missing"
		)
		val provider = listOf(featureTargeted, baseTargeted, createBaseApk()).toSplitPackage()

		val sorted = provider.sortedByCompatibility(context).getAsync().get()

		val apk = sorted.screenDensity.first().apk
		assertTrue(apk.configForSplit.isEmpty())
		assertEquals("config.xxhdpi", apk.name)
	}

	@Test
	fun sortedByCompatibilitySortsDynamicFeatureEntries() {
		val feature = createFeatureApk(name = "camera")
		val ldpi = createScreenDensityApk(name = "camera.config.ldpi", dpi = Dpi.LDPI, configForSplit = "camera")
		val xxhdpi = createScreenDensityApk(name = "camera.config.xxhdpi", dpi = Dpi.XXHDPI, configForSplit = "camera")
		val provider = listOf(feature, ldpi, xxhdpi, createBaseApk()).toSplitPackage()

		val sorted = provider.sortedByCompatibility(context).getAsync().get()
		val featureDensity = sorted.dynamicFeatures.single().screenDensity

		assertEquals(Dpi.XXHDPI, featureDensity.first().apk.dpi)
		assertTrue(featureDensity.first().isPreferred)
		assertFalse(featureDensity.last().isPreferred)
	}

	@Test
	fun filterCompatibleReturnsOnlyPreferredEntries() {
		val ldpi = createScreenDensityApk(name = "config.ldpi", dpi = Dpi.LDPI)
		val xxhdpi = createScreenDensityApk(name = "config.xxhdpi", dpi = Dpi.XXHDPI)
		val provider = listOf(ldpi, xxhdpi, createBaseApk()).toSplitPackage()

		val filtered = provider.filterCompatible(context).getAsync().get()

		assertEquals(1, filtered.screenDensity.size)
		assertEquals(Dpi.XXHDPI, filtered.screenDensity.single().apk.dpi)
	}

	@Test
	fun filterCompatibleRemovesIncompatibleEntries() {
		val libs = createLibsApk(name = "config.x86", abi = Abi.X86)
		val provider = listOf(libs, createBaseApk()).toSplitPackage()

		val filtered = provider.filterCompatible(context).getAsync().get()

		assertTrue(filtered.libs.isEmpty())
	}

	@Test
	fun filterCompatibleFiltersDynamicFeatureEntries() {
		val feature = createFeatureApk(name = "camera")
		val ldpi = createScreenDensityApk(name = "camera.config.ldpi", dpi = Dpi.LDPI, configForSplit = "camera")
		val xxhdpi = createScreenDensityApk(name = "camera.config.xxhdpi", dpi = Dpi.XXHDPI, configForSplit = "camera")
		val provider = listOf(feature, ldpi, xxhdpi, createBaseApk()).toSplitPackage()

		val filtered = provider.filterCompatible(context).getAsync().get()

		val dynamicFeature = filtered.dynamicFeatures.single()
		assertEquals(1, dynamicFeature.screenDensity.size)
		assertEquals(Dpi.XXHDPI, dynamicFeature.screenDensity.single().apk.dpi)
	}

	private fun createProvider(): SplitPackage.Provider {
		return listOf(createBaseApk(), createLibsApk(), createScreenDensityApk()).toSplitPackage()
	}
}