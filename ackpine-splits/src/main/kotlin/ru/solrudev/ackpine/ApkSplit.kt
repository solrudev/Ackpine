package ru.solrudev.ackpine

import android.content.Context
import android.net.Uri
import ru.solrudev.ackpine.Dpi.Companion.dpi
import ru.solrudev.ackpine.helpers.deviceLocales
import ru.solrudev.ackpine.helpers.name
import ru.solrudev.ackpine.helpers.splitTypePart
import java.io.File
import java.util.IllformedLocaleException
import java.util.Locale
import java.util.zip.ZipEntry

public sealed class ApkSplit(
	public open val uri: Uri,
	public open val name: String,
	public open val description: String
) {

	public data class Other(
		override val uri: Uri,
		override val name: String
	) : ApkSplit(uri, name, description = name) {

		override fun isCompatible(context: Context): Boolean {
			return true
		}
	}

	public data class Libs(
		override val uri: Uri,
		override val name: String,
		public val abi: Abi
	) : ApkSplit(uri, name, description = abi.name.lowercase()) {

		override fun isCompatible(context: Context): Boolean {
			return abi in Abi.deviceAbis
		}
	}

	public data class ScreenDensity(
		override val uri: Uri,
		override val name: String,
		public val dpi: Dpi
	) : ApkSplit(uri, name, description = dpi.name.lowercase()) {

		override fun isCompatible(context: Context): Boolean {
			return dpi == context.dpi
		}
	}

	public data class Localization(
		override val uri: Uri,
		override val name: String,
		public val locale: Locale
	) : ApkSplit(uri, name, description = "") {

		override val description: String
			get() = locale.displayLanguage

		override fun isCompatible(context: Context): Boolean {
			return locale.language in deviceLocales(context).map { it.language }
		}
	}

	public abstract fun isCompatible(context: Context): Boolean

	public companion object {

		@JvmStatic
		public fun fromFile(file: File): ApkSplit? {
			if (!file.isApk) {
				return null
			}
			val uri = Uri.fromFile(file)
			val name = file.name.substringAfterLast('/').substringBeforeLast('.')
			return fromName(name, uri)
		}

		@JvmStatic
		public fun fromUri(context: Context, uri: Uri): ApkSplit? {
			if (!uri.isApk(context)) {
				return null
			}
			val name = uri.name(context)?.substringAfterLast('/')?.substringBeforeLast('.') ?: return null
			return fromName(name, uri)
		}

		@JvmSynthetic
		internal fun fromZipEntry(zipPath: String, zipEntry: ZipEntry): ApkSplit? {
			if (!zipEntry.isApk) {
				return null
			}
			val uri = ZippedFileProvider.getUriForZipEntry(zipPath, zipEntry.name)
			val name = zipEntry.name.substringAfterLast('/').substringBeforeLast('.')
			return fromName(name, uri)
		}

		private fun fromName(name: String, uri: Uri): ApkSplit {
			val dpi = Dpi.fromSplitName(name)
			val abi = Abi.fromSplitName(name)
			val locale = localeFromSplitName(name)
			return when {
				dpi != null -> ScreenDensity(uri, name, dpi)
				abi != null -> Libs(uri, name, abi)
				locale != null -> Localization(uri, name, locale)
				else -> Other(uri, name)
			}
		}

		private fun localeFromSplitName(name: String): Locale? {
			val localePart = splitTypePart(name) ?: return null
			val locale = try {
				Locale.Builder().setLanguageTag(localePart).build()
			} catch (_: IllformedLocaleException) {
				null
			}
			return Locale.getAvailableLocales().firstOrNull { it == locale }
		}

		private val ZipEntry.isApk: Boolean
			get() = name.endsWith(".apk", ignoreCase = true) && !isDirectory

		private val File.isApk: Boolean
			get() = name.endsWith(".apk", ignoreCase = true) && !isDirectory

		private fun Uri.isApk(context: Context): Boolean {
			return context.contentResolver.getType(this) == "application/vnd.android.package-archive"
		}
	}
}