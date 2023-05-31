package ru.solrudev.ackpine.helpers

import android.content.Context
import androidx.core.os.ConfigurationCompat
import java.util.IllformedLocaleException
import java.util.Locale

@JvmSynthetic
internal fun deviceLocales(context: Context): List<Locale> {
	val locales = ConfigurationCompat.getLocales(context.resources.configuration)
	return buildList {
		for (index in 0 until locales.size()) {
			add(locales[index]!!)
		}
	}
}

@JvmSynthetic
internal fun localeFromSplitName(name: String): Locale? {
	val localePart = splitTypePart(name) ?: return null
	val locale = try {
		Locale.Builder().setLanguageTag(localePart).build()
	} catch (_: IllformedLocaleException) {
		null
	}
	return Locale.getAvailableLocales().firstOrNull { it == locale }
}