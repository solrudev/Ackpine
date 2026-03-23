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
import ru.solrudev.ackpine.splits.Dpi.Companion.dpi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class DpiTest {

	private val context: Context = ApplicationProvider.getApplicationContext()

	@Test
	fun fromSplitNameReturnsCorrectDpiForAllEntries() {
		for (dpi in Dpi.entries) {
			val splitName = "config.${dpi.name.lowercase()}"
			assertEquals(dpi, Dpi.fromSplitName(splitName))
		}
	}

	@Test
	fun fromSplitNameReturnsNullForUnknownDpi() {
		assertNull(Dpi.fromSplitName("config.unknown"))
	}

	@Test
	fun fromSplitNameReturnsNullForNoConfigPrefix() {
		assertNull(Dpi.fromSplitName("hdpi"))
	}

	@Test
	fun fromSplitNameIsCaseInsensitive() {
		assertEquals(Dpi.XXHDPI, Dpi.fromSplitName("config.XxHdPi"))
	}

	@Test
	fun dpiReturnsExactOrHigherDpiWithLowerFallback() {
		assertDpi(Dpi.XXHDPI.density, Dpi.XXHDPI)
		assertDpi(Dpi.HDPI.density + 1, Dpi.XHDPI)
		assertDpi(Dpi.LDPI.density - 1, Dpi.LDPI)
		assertDpi(Dpi.XXXHDPI.density + 1, Dpi.XXXHDPI)
	}

	private fun assertDpi(density: Int, expected: Dpi) {
		context.resources.displayMetrics.densityDpi = density
		assertEquals(expected, context.dpi)
	}
}