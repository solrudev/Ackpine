/*
 * Copyright (C) 2023 Ilya Fomichev
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

import android.content.Context
import androidx.core.os.ConfigurationCompat
import java.util.IllformedLocaleException
import java.util.Locale

private val availableLocales = Locale.getAvailableLocales()
	.asSequence()
	.filter { it.language.isNotEmpty() }
	.toSet()

@JvmSynthetic
internal fun deviceLocales(context: Context): List<Locale> {
	val locales = ConfigurationCompat.getLocales(context.resources.configuration)
	return buildList {
		for (index in 0..<locales.size()) {
			add(locales[index]!!)
		}
	}
}

@JvmSynthetic
internal fun localeFromSplitName(name: String): Locale? {
	val localePart = splitTypePart(name) ?: return null
	val tag = localePart.replace(oldChar = '_', newChar = '-')
	val locale = try {
		Locale.Builder().setLanguageTag(tag).build()
	} catch (_: IllformedLocaleException) {
		return null
	}
	if (locale.language.isEmpty()) {
		return null
	}
	return locale.takeIf { it in availableLocales }
}

@JvmSynthetic
internal fun Locale.comparator(deviceLocales: List<Locale>) = deviceLocales
	.withIndex()
	.minOfOrNull { (index, deviceLocale) ->
		if (language.isEmpty() || language != deviceLocale.language) {
			return@minOfOrNull Int.MAX_VALUE
		}
		val matchBase = index * 5
		if (toLanguageTag() == deviceLocale.toLanguageTag()) {
			return@minOfOrNull matchBase
		}
		val scriptMatches = script.isNotEmpty() && script == deviceLocale.script
		val countryMatches = country.isNotEmpty() && country == deviceLocale.country
		when {
			scriptMatches && countryMatches -> matchBase + 1
			scriptMatches -> matchBase + 2
			countryMatches -> matchBase + 3
			else -> matchBase + 4
		}
	} ?: Int.MAX_VALUE