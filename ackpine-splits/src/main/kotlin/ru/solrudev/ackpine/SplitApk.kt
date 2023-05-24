package ru.solrudev.ackpine

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.DisplayMetrics
import androidx.core.os.ConfigurationCompat
import ru.solrudev.ackpine.Dpi.Companion.dpi
import java.util.IllformedLocaleException
import java.util.Locale
import java.util.zip.ZipEntry

private const val CONFIG_PART = "config."
private val dpis = Dpi.values().map { it.name.lowercase() }.toSet()
private val abis = Abi.values().map { it.name.lowercase() }.toSet()

public sealed class SplitApk(
	public open val uri: Uri,
	public open val name: String,
	public val description: String
) {

	public data class Other(
		override val uri: Uri,
		override val name: String
	) : SplitApk(uri, name, name)

	public data class Resources(
		override val uri: Uri,
		override val name: String,
		public val dpi: Dpi
	) : SplitApk(uri, name, dpi.name.lowercase())

	public data class Libs(
		override val uri: Uri,
		override val name: String,
		public val abi: Abi
	) : SplitApk(uri, name, abi.name.lowercase())

	public data class Localization(
		override val uri: Uri,
		override val name: String,
		public val locale: Locale
	) : SplitApk(uri, name, locale.displayLanguage)

	@JvmSynthetic
	internal fun isCompatible(context: Context): Boolean = when (this) {
		is Other -> true
		is Libs -> abi in Abi.deviceAbis()
		is Resources -> dpi == context.dpi
		is Localization -> locale.languageEquals(
			ConfigurationCompat.getLocales(context.resources.configuration)[0] ?: Locale.ENGLISH
		)
	}

	internal companion object {

		private val ZipEntry.isApk: Boolean
			get() = name.endsWith(".apk", ignoreCase = true) && !isDirectory

		@JvmSynthetic
		internal fun fromZipEntry(zipPath: String, zipEntry: ZipEntry): SplitApk? {
			if (!zipEntry.isApk) {
				return null
			}
			val uri = ZippedFileProvider.getUriForZipEntry(zipPath, zipEntry.name)
			val name = zipEntry.name.substringAfterLast('/').substringBeforeLast('.')
			val dpi = Dpi.fromSplitName(name)
			val abi = Abi.fromSplitName(name)
			val locale = localeFromSplitName(name)
			return when {
				dpi != null -> Resources(uri, name, dpi)
				abi != null -> Libs(uri, name, abi)
				locale != null -> Localization(uri, name, locale)
				else -> Other(uri, name)
			}
		}

		private fun localeFromSplitName(name: String): Locale? {
			val localePart = typePart(name) ?: return null
			val locale = try {
				Locale.Builder().setLanguageTag(localePart).build()
			} catch (_: IllformedLocaleException) {
				null
			}
			return Locale.getAvailableLocales().firstOrNull { it == locale }
		}
	}
}

public enum class Abi {

	ARMEABI, ARMEABI_V7A, ARM64_V8A, X86, X86_64, MIPS, MIPS64;

	internal companion object {

		@JvmSynthetic
		internal fun deviceAbis(): List<Abi> = listOf(
			Build.SUPPORTED_ABIS.first().replace(oldChar = '-', newChar = '_').uppercase(),
			Build.SUPPORTED_ABIS.getOrNull(1).orEmpty().replace(oldChar = '-', newChar = '_').uppercase(),
			Build.SUPPORTED_ABIS.getOrNull(2).orEmpty().replace(oldChar = '-', newChar = '_').uppercase()
		)
			.filter { it.isNotEmpty() }
			.map { valueOf(it) }

		@JvmSynthetic
		internal fun fromSplitName(name: String): Abi? {
			val abiPart = typePart(name) ?: return null
			return if (abiPart in abis) {
				Abi.valueOf(abiPart.uppercase())
			} else {
				null
			}
		}
	}
}

public enum class Dpi {

	LDPI, MDPI, TVDPI, HDPI, XHDPI, XXHDPI, XXXHDPI;

	internal companion object {

		@get:JvmSynthetic
		internal val Context.dpi: Dpi
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
			val dpiPart = typePart(name) ?: return null
			return if (dpiPart in dpis) {
				Dpi.valueOf(dpiPart.uppercase())
			} else {
				null
			}
		}
	}
}

private fun typePart(name: String): String? {
	if (!name.contains(CONFIG_PART, ignoreCase = true) && !name.contains(".$CONFIG_PART", ignoreCase = true)) {
		return null
	}
	return name.substringAfter(CONFIG_PART).lowercase()
}

private fun Locale.languageEquals(other: Locale): Boolean {
	return language == other.language
}