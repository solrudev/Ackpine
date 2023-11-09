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

package ru.solrudev.ackpine.splits

import android.content.Context
import android.util.DisplayMetrics
import ru.solrudev.ackpine.splits.helpers.splitTypePart

/**
 * Density of [APK split][Apk] graphic resources.
 *
 * @property density Dots-per-inch value.
 */
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

		/**
		 * Returns device's screen density expressed as [Dpi] enum entry.
		 */
		@JvmStatic
		@get:JvmName("fromContext")
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