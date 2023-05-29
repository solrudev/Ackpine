package ru.solrudev.ackpine.helpers

import android.content.Context
import androidx.core.os.ConfigurationCompat
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