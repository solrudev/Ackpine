package ru.solrudev.ackpine

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.os.ConfigurationCompat
import ru.solrudev.ackpine.Dpi.Companion.dpi
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
	) : ApkSplit(uri, name, name)

	public data class ScreenDensity(
		override val uri: Uri,
		override val name: String,
		public val dpi: Dpi
	) : ApkSplit(uri, name, dpi.name.lowercase())

	public data class Libs(
		override val uri: Uri,
		override val name: String,
		public val abi: Abi
	) : ApkSplit(uri, name, abi.name.lowercase())

	public data class Localization(
		override val uri: Uri,
		override val name: String,
		public val locale: Locale
	) : ApkSplit(uri, name, "") {

		override val description: String
			get() = locale.displayLanguage
	}

	@JvmSynthetic
	internal fun isCompatible(context: Context): Boolean = when (this) {
		is Other -> true
		is Libs -> abi in Abi.deviceAbis
		is ScreenDensity -> dpi == context.dpi
		is Localization -> locale.languageEquals(
			ConfigurationCompat.getLocales(context.resources.configuration)[0] ?: Locale.ENGLISH
		)
	}

	private fun Locale.languageEquals(other: Locale): Boolean {
		return language == other.language
	}

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

		private fun Uri.name(context: Context): String? {
			return context.contentResolver.query(
				this,
				arrayOf(OpenableColumns.DISPLAY_NAME),
				null, null, null
			)?.use { cursor ->
				if (!cursor.moveToFirst()) {
					return null
				}
				return cursor.getString(0)
			}
		}
	}
}