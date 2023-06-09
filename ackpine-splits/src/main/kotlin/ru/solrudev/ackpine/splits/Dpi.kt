package ru.solrudev.ackpine.splits

import android.content.Context
import android.util.DisplayMetrics
import ru.solrudev.ackpine.helpers.splitTypePart

public enum class Dpi(public val density: Int) {

	LDPI(DisplayMetrics.DENSITY_LOW),
	MDPI(DisplayMetrics.DENSITY_MEDIUM),
	TVDPI(DisplayMetrics.DENSITY_TV),
	HDPI(DisplayMetrics.DENSITY_HIGH),
	XHDPI(DisplayMetrics.DENSITY_XHIGH),
	XXHDPI(DisplayMetrics.DENSITY_XXHIGH),
	XXXHDPI(DisplayMetrics.DENSITY_XXXHIGH);

	public companion object {

		private val dpis = Dpi.values().map { it.name.lowercase() }.toSet()

		@JvmStatic
		public val Context.dpi: Dpi
			get() {
				val dpi = resources.displayMetrics.densityDpi
				return when {
					dpi == DisplayMetrics.DENSITY_TV -> TVDPI
					dpi <= DisplayMetrics.DENSITY_LOW -> LDPI
					dpi <= DisplayMetrics.DENSITY_MEDIUM -> MDPI
					dpi <= DisplayMetrics.DENSITY_HIGH -> HDPI
					dpi <= DisplayMetrics.DENSITY_XHIGH -> XHDPI
					dpi <= DisplayMetrics.DENSITY_XXHIGH -> XXHDPI
					dpi <= DisplayMetrics.DENSITY_XXXHIGH -> XXXHDPI
					else -> XXXHDPI
				}
			}

		@JvmSynthetic
		internal fun fromSplitName(name: String): Dpi? {
			val dpiPart = splitTypePart(name) ?: return null
			if (dpiPart in dpis) {
				return Dpi.valueOf(dpiPart.uppercase())
			}
			return null
		}
	}
}