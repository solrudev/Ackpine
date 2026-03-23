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

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LocaleHelpersTest {

	private val deviceLocales = listOf(Locale("en", "US"), Locale("fr", "FR"), Locale("de", "DE"))

	@Test
	fun localeFromSplitNameReturnsLocaleForLanguage() {
		val locale = localeFromSplitName("config.en")
		assertNotNull(locale)
		assertEquals("en", locale.language)
	}

	@Test
	fun localeFromSplitNameReturnsLanguageLocaleForRegionalTag() {
		val locale = localeFromSplitName("config.en_us")
		assertNotNull(locale)
		assertEquals("en", locale.language)
		assertEquals("", locale.country)
	}

	@Test
	fun localeFromSplitNameReturnsNullForNoConfigPrefix() {
		assertNull(localeFromSplitName("en"))
	}

	@Test
	fun localeFromSplitNameReturnsNullForInvalidTag() {
		assertNull(localeFromSplitName("config.123"))
	}

	@Test
	fun localeFromSplitNameReturnsNullForPrivateUseOnlyTag() {
		assertNull(localeFromSplitName("config.x_foo"))
	}

	@Test
	fun matchScoreReturnsMaxValueForEmptyDeviceLocales() {
		assertEquals(Int.MAX_VALUE, Locale("en").matchScore(emptyList()))
	}

	@Test
	fun matchScoreReturnsMaxValueForNonMatchingLanguage() {
		assertEquals(Int.MAX_VALUE, Locale("ja").matchScore(deviceLocales))
	}

	@Test
	fun matchScoreReturnsMaxValueForEmptyLanguage() {
		assertEquals(Int.MAX_VALUE, Locale.ROOT.matchScore(deviceLocales))
	}

	@Test
	fun matchScoreIgnoresCountry() {
		assertEquals(
			Locale("en", "GB").matchScore(listOf(Locale("en", "US"))),
			Locale("en").matchScore(listOf(Locale("en", "US")))
		)
	}

	@Test
	fun matchScoreEarlierExactMatchIsBetterThanLaterExactMatch() {
		val english = Locale("en", "US").matchScore(deviceLocales)
		val french = Locale("fr", "FR").matchScore(deviceLocales)
		assertTrue(english < french)
	}

	@Test
	fun matchScoreEarlierLanguageOnlyMatchIsBetterThanLaterLanguageOnlyMatch() {
		val english = Locale("en", "GB").matchScore(deviceLocales)
		val german = Locale("de", "AT").matchScore(deviceLocales)
		assertTrue(english < german)
	}

	@Test
	fun matchScoreEarlierPositionWithLanguageOnlyMatchBeatsLaterExactMatch() {
		val locales = listOf(Locale("en"), Locale("fr", "FR"))
		val englishLanguageOnly = Locale("en", "GB").matchScore(locales)
		val frenchExact = Locale("fr", "FR").matchScore(locales)
		assertTrue(englishLanguageOnly < frenchExact)
	}
}