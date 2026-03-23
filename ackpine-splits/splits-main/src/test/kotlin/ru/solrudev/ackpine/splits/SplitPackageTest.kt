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

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import ru.solrudev.ackpine.splits.ApkSplits.validate
import ru.solrudev.ackpine.splits.SplitPackage.Companion.toSplitPackage
import ru.solrudev.ackpine.splits.testutil.createBaseApk
import ru.solrudev.ackpine.splits.testutil.createFeatureApk
import ru.solrudev.ackpine.splits.testutil.createLibsApk
import ru.solrudev.ackpine.splits.testutil.createLocalizationApk
import ru.solrudev.ackpine.splits.testutil.createOtherApk
import ru.solrudev.ackpine.splits.testutil.createScreenDensityApk
import java.util.concurrent.ExecutionException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

@RunWith(RobolectricTestRunner::class)
class SplitPackageTest {

	@Test
	fun emptyProviderReturnsEmptySplitPackage() {
		val splitPackage = SplitPackage.empty().getAsync().get()
		assertTrue(splitPackage.base.isEmpty())
		assertTrue(splitPackage.libs.isEmpty())
		assertTrue(splitPackage.screenDensity.isEmpty())
		assertTrue(splitPackage.localization.isEmpty())
		assertTrue(splitPackage.other.isEmpty())
		assertTrue(splitPackage.dynamicFeatures.isEmpty())
	}

	@Test
	fun toSplitPackageCategorizesAllApkTypes() {
		val base = createBaseApk()
		val libs = createLibsApk()
		val density = createScreenDensityApk()
		val localization = createLocalizationApk()
		val other = createOtherApk()
		val splitPackage = sequenceOf(base, libs, density, localization, other)
			.toSplitPackage()
			.getAsync()
			.get()
		assertEquals(base, splitPackage.base.single().apk)
		assertEquals(libs, splitPackage.libs.single().apk)
		assertEquals(density, splitPackage.screenDensity.single().apk)
		assertEquals(localization, splitPackage.localization.single().apk)
		assertEquals(other, splitPackage.other.single().apk)
	}

	@Test
	fun toSplitPackageGroupsDynamicFeature() {
		val feature = createFeatureApk(name = "camera")
		val featureLibs = createLibsApk(name = "camera.config.arm64_v8a", configForSplit = "camera")
		val featureDensity = createScreenDensityApk(
			name = "camera.config.xxhdpi",
			configForSplit = "camera"
		)
		val featureLocalization = createLocalizationApk(
			name = "camera.config.en",
			configForSplit = "camera"
		)
		val splitPackage = sequenceOf(feature, featureLibs, featureDensity, featureLocalization)
			.toSplitPackage()
			.getAsync()
			.get()
		val dynamicFeature = splitPackage.dynamicFeatures.single()
		assertEquals(feature, dynamicFeature.feature)
		assertEquals(featureLibs, dynamicFeature.libs.single().apk)
		assertEquals(featureDensity, dynamicFeature.screenDensity.single().apk)
		assertEquals(featureLocalization, dynamicFeature.localization.single().apk)
		assertTrue(splitPackage.libs.isEmpty())
		assertTrue(splitPackage.screenDensity.isEmpty())
		assertTrue(splitPackage.localization.isEmpty())
	}

	@Test
	fun toSplitPackageKeepsConfigSplitsAtTopLevelWhenFeatureIsMissing() {
		val libs = createLibsApk(name = "missing.config.arm64_v8a", configForSplit = "missing")
		val base = createBaseApk()
		val splitPackage = sequenceOf(base, libs).toSplitPackage().getAsync().get()
		assertEquals(1, splitPackage.libs.size)
		assertTrue(splitPackage.dynamicFeatures.isEmpty())
	}

	@Test
	fun toSplitPackagePropagatesSequenceException() {
		val error = RuntimeException("test error")
		val sequence = sequence<Apk> {
			throw error
		}

		val future = sequence.toSplitPackage().getAsync()

		val exception = assertFailsWith<ExecutionException> {
			future.get()
		}
		assertSame(error, exception.cause)
	}

	@Test
	fun toSplitPackagePropagatesCancellation() = runTest {
		val firstEmit = CompletableDeferred<Unit>()
		val resourceClosed = CompletableDeferred<Unit>()
		val resource = AutoCloseable { resourceClosed.complete(Unit) }

		val source = closeableSequence {
			resource.use()
			yield(createBaseApk())
			firstEmit.complete(Unit)
			@Suppress("RunBlockingInSuspendFunction")
			runBlocking { resourceClosed.await() }
			if (isClosed) {
				return@closeableSequence
			}
			yield(createLibsApk())
		}

		val validated = source.validate()
		val future = validated.toSplitPackage().getAsync()

		withTimeout(2.seconds) {
			firstEmit.await()
			assertTrue(future.cancel(false))
			resourceClosed.await()

			assertTrue(validated.isClosed)
			assertTrue(source.isClosed)
			assertTrue(future.isCancelled)
		}
	}

	@Test
	fun filterPreferredFiltersNonPreferredEntries() {
		val preferred = SplitPackage.Entry(isPreferred = true, createLibsApk(name = "config.arm64_v8a"))
		val nonPreferred = SplitPackage.Entry(isPreferred = false, createLibsApk(name = "config.x86", abi = Abi.X86))
		val splitPackage = SplitPackage(
			base = listOf(SplitPackage.Entry(isPreferred = true, createBaseApk())),
			libs = listOf(preferred, nonPreferred),
			screenDensity = emptyList(),
			localization = emptyList(),
			other = emptyList(),
			dynamicFeatures = emptyList()
		)

		val filtered = splitPackage.filterPreferred()

		assertEquals(preferred.apk, filtered.libs.single().apk)
	}

	@Test
	fun filterPreferredIsIdempotent() {
		val splitPackage = SplitPackage(
			base = listOf(SplitPackage.Entry(isPreferred = true, createBaseApk())),
			libs = listOf(SplitPackage.Entry(isPreferred = true, createLibsApk())),
			screenDensity = emptyList(),
			localization = emptyList(),
			other = emptyList(),
			dynamicFeatures = emptyList()
		)

		val filtered = splitPackage.filterPreferred()

		assertSame(filtered, filtered.filterPreferred())
	}

	@Test
	fun filterPreferredFiltersDynamicFeatureEntries() {
		val feature = createFeatureApk(name = "camera")
		val preferred = SplitPackage.Entry(isPreferred = true, createLibsApk(name = "camera.config.arm64_v8a"))
		val nonPreferred = SplitPackage.Entry(
			isPreferred = false,
			createLibsApk(name = "camera.config.x86", abi = Abi.X86)
		)
		val splitPackage = SplitPackage(
			base = emptyList(),
			libs = emptyList(),
			screenDensity = emptyList(),
			localization = emptyList(),
			other = emptyList(),
			dynamicFeatures = listOf(
				SplitPackage.DynamicFeature(
					feature = feature,
					libs = listOf(preferred, nonPreferred),
					screenDensity = emptyList(),
					localization = emptyList()
				)
			)
		)

		val filtered = splitPackage.filterPreferred()

		assertEquals(preferred.apk, filtered.dynamicFeatures.single().libs.single().apk)
	}

	@Test
	fun toListFlattensAllEntries() {
		val base = createBaseApk()
		val libs = createLibsApk()
		val density = createScreenDensityApk()
		val localization = createLocalizationApk()
		val other = createOtherApk()
		val feature = createFeatureApk(name = "camera")
		val featureLibs = createLibsApk(name = "camera.config.arm64_v8a")
		val splitPackage = SplitPackage(
			base = listOf(SplitPackage.Entry(isPreferred = true, base)),
			libs = listOf(SplitPackage.Entry(isPreferred = true, libs)),
			screenDensity = listOf(SplitPackage.Entry(isPreferred = true, density)),
			localization = listOf(SplitPackage.Entry(isPreferred = true, localization)),
			other = listOf(SplitPackage.Entry(isPreferred = true, other)),
			dynamicFeatures = listOf(
				SplitPackage.DynamicFeature(
					feature = feature,
					libs = listOf(SplitPackage.Entry(isPreferred = true, featureLibs)),
					screenDensity = emptyList(),
					localization = emptyList()
				)
			)
		)

		val apks = splitPackage.toList().map { it.apk }

		assertEquals(listOf(base, libs, density, localization, other, feature, featureLibs), apks)
	}

	@Test
	fun equalSplitPackagesAreEqual() {
		val base = listOf(SplitPackage.Entry(isPreferred = true, createBaseApk()))
		val first = SplitPackage(base, emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
		val second = SplitPackage(base, emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
		assertEquals(first, second)
	}

	@Test
	fun differentSplitPackagesAreNotEqual() {
		val first = SplitPackage(
			listOf(SplitPackage.Entry(isPreferred = true, createBaseApk())),
			emptyList(), emptyList(), emptyList(), emptyList(), emptyList()
		)
		val second = SplitPackage(
			emptyList(),
			listOf(SplitPackage.Entry(isPreferred = true, createLibsApk())),
			emptyList(), emptyList(), emptyList(), emptyList()
		)
		assertNotEquals(first, second)
	}
}