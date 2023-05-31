package ru.solrudev.ackpine

import android.content.Context
import android.net.Uri
import ru.solrudev.ackpine.Dpi.Companion.dpi
import ru.solrudev.ackpine.helpers.NonClosingInputStream.Companion.nonClosing
import ru.solrudev.ackpine.helpers.deviceLocales
import ru.solrudev.ackpine.helpers.displayNameAndSize
import ru.solrudev.ackpine.helpers.isApk
import ru.solrudev.ackpine.helpers.localeFromSplitName
import ru.solrudev.ackpine.helpers.toFile
import ru.solrudev.ackpine.parsing.AndroidManifest
import ru.solrudev.ackpine.parsing.androidManifest
import ru.solrudev.ackpine.parsing.parseAndroidManifest
import java.io.File
import java.io.InputStream
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

public sealed class ApkSplit(
	public open val uri: Uri,
	public open val name: String,
	public open val size: Long,
	public open val packageName: String,
	public open val versionCode: Long,
	public open val description: String
) {

	public data class Base(
		override val uri: Uri,
		override val name: String,
		override val size: Long,
		override val packageName: String,
		override val versionCode: Long,
		public val versionName: String
	) : ApkSplit(uri, name, size, packageName, versionCode, description = name) {
		override fun isCompatible(context: Context): Boolean = true
	}

	public data class Feature(
		override val uri: Uri,
		override val name: String,
		override val size: Long,
		override val packageName: String,
		override val versionCode: Long
	) : ApkSplit(uri, name, size, packageName, versionCode, description = name) {
		override fun isCompatible(context: Context): Boolean = true
	}

	public data class Libs(
		override val uri: Uri,
		override val name: String,
		override val size: Long,
		override val packageName: String,
		override val versionCode: Long,
		public val abi: Abi
	) : ApkSplit(uri, name, size, packageName, versionCode, description = abi.name.lowercase()) {
		override fun isCompatible(context: Context): Boolean = abi in Abi.deviceAbis
	}

	public data class ScreenDensity(
		override val uri: Uri,
		override val name: String,
		override val size: Long,
		override val packageName: String,
		override val versionCode: Long,
		public val dpi: Dpi
	) : ApkSplit(uri, name, size, packageName, versionCode, description = dpi.name.lowercase()) {
		override fun isCompatible(context: Context): Boolean = dpi == context.dpi
	}

	public data class Localization(
		override val uri: Uri,
		override val name: String,
		override val size: Long,
		override val packageName: String,
		override val versionCode: Long,
		public val locale: Locale
	) : ApkSplit(uri, name, size, packageName, versionCode, description = "") {

		override val description: String
			get() = locale.displayLanguage

		override fun isCompatible(context: Context): Boolean {
			return locale.language in deviceLocales(context).map { it.language }
		}
	}

	public data class Other(
		override val uri: Uri,
		override val name: String,
		override val size: Long,
		override val packageName: String,
		override val versionCode: Long
	) : ApkSplit(uri, name, size, packageName, versionCode, description = name) {
		override fun isCompatible(context: Context): Boolean = true
	}

	public abstract fun isCompatible(context: Context): Boolean

	public companion object {

		@JvmStatic
		public fun fromFile(file: File): ApkSplit? {
			if (!file.isApk) {
				return null
			}
			val name = file.name.substringAfterLast('/').substringBeforeLast('.')
			file.inputStream().use { inputStream ->
				return createApkSplit(inputStream, name, Uri.fromFile(file), file.length())
			}
		}

		@JvmStatic
		public fun fromUri(context: Context, uri: Uri): ApkSplit? {
			val file = uri.toFile(context)
			if (file.canRead()) {
				return fromFile(file)
			}
			if (!uri.isApk(context)) {
				return null
			}
			val (displayName, size) = uri.displayNameAndSize(context)
			val name = displayName.substringAfterLast('/').substringBeforeLast('.')
			context.contentResolver.openInputStream(uri).use { inputStream ->
				inputStream ?: return null
				return createApkSplit(inputStream, name, uri, size)
			}
		}

		@JvmSynthetic
		internal fun fromZipEntry(zipPath: String, zipEntry: ZipEntry, inputStream: InputStream): ApkSplit? {
			if (!zipEntry.isApk) {
				return null
			}
			val uri = ZippedFileProvider.getUriForZipEntry(zipPath, zipEntry.name)
			val name = zipEntry.name.substringAfterLast('/').substringBeforeLast('.')
			return createApkSplit(inputStream, name, uri, zipEntry.size)
		}

		private fun createApkSplit(inputStream: InputStream, name: String, uri: Uri, size: Long): ApkSplit? {
			val androidManifest = ZipInputStream(inputStream.nonClosing()).use { it.androidManifest() } ?: return null
			val manifest = parseAndroidManifest(androidManifest) ?: return null
			return when {
				manifest.splitName.isEmpty() -> {
					Base(uri, name, size, manifest.packageName, manifest.versionCode, manifest.versionName)
				}

				manifest.isFeatureSplit -> {
					Feature(uri, manifest.splitName, size, manifest.packageName, manifest.versionCode)
				}

				manifest.splitName.startsWith("config") || manifest.configForSplit.isNotEmpty() -> {
					configApkSplit(manifest, uri, size)
				}

				else -> null
			}
		}

		private fun configApkSplit(manifest: AndroidManifest, uri: Uri, size: Long): ApkSplit {
			val splitName = manifest.splitName
			val dpi = Dpi.fromSplitName(splitName)
			val abi = Abi.fromSplitName(splitName)
			val locale = localeFromSplitName(splitName)
			return when {
				dpi != null -> ScreenDensity(uri, splitName, size, manifest.packageName, manifest.versionCode, dpi)
				abi != null -> Libs(uri, splitName, size, manifest.packageName, manifest.versionCode, abi)
				locale != null -> Localization(uri, splitName, size, manifest.packageName, manifest.versionCode, locale)
				else -> Other(uri, splitName, size, manifest.packageName, manifest.versionCode)
			}
		}
	}
}