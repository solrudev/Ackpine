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
import android.net.Uri
import androidx.core.content.FileProvider
import ru.solrudev.ackpine.ZippedFileProvider
import ru.solrudev.ackpine.helpers.NonClosingInputStream.Companion.nonClosing
import ru.solrudev.ackpine.helpers.deviceLocales
import ru.solrudev.ackpine.helpers.displayNameAndSize
import ru.solrudev.ackpine.helpers.isApk
import ru.solrudev.ackpine.helpers.localeFromSplitName
import ru.solrudev.ackpine.helpers.toFile
import ru.solrudev.ackpine.splits.Dpi.Companion.dpi
import ru.solrudev.ackpine.splits.parsing.AndroidManifest
import ru.solrudev.ackpine.splits.parsing.androidManifest
import ru.solrudev.ackpine.splits.parsing.parseAndroidManifest
import java.io.File
import java.io.InputStream
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * Represents an APK split.
 */
public sealed class Apk(
	public open val uri: Uri,
	public open val name: String,
	public open val size: Long,
	public open val packageName: String,
	public open val versionCode: Long,
	public open val description: String
) {

	/**
	 * Base APK.
	 */
	public data class Base(
		override val uri: Uri,
		override val name: String,
		override val size: Long,
		override val packageName: String,
		override val versionCode: Long,
		public val versionName: String
	) : Apk(uri, name, size, packageName, versionCode, description = name) {
		override fun isCompatible(context: Context): Boolean = true
	}

	/**
	 * Feature split.
	 */
	public data class Feature(
		override val uri: Uri,
		override val name: String,
		override val size: Long,
		override val packageName: String,
		override val versionCode: Long
	) : Apk(uri, name, size, packageName, versionCode, description = name) {
		override fun isCompatible(context: Context): Boolean = true
	}

	/**
	 * APK split containing native libraries.
	 */
	public data class Libs(
		override val uri: Uri,
		override val name: String,
		override val size: Long,
		override val packageName: String,
		override val versionCode: Long,
		public val abi: Abi
	) : Apk(uri, name, size, packageName, versionCode, description = abi.name.lowercase()) {
		override fun isCompatible(context: Context): Boolean = abi in Abi.deviceAbis
	}

	/**
	 * APK split containing graphic resources tailored to specific screen density.
	 */
	public data class ScreenDensity(
		override val uri: Uri,
		override val name: String,
		override val size: Long,
		override val packageName: String,
		override val versionCode: Long,
		public val dpi: Dpi
	) : Apk(uri, name, size, packageName, versionCode, description = dpi.name.lowercase()) {
		override fun isCompatible(context: Context): Boolean = dpi == context.dpi
	}

	/**
	 * APK split containing localized resources.
	 */
	public data class Localization(
		override val uri: Uri,
		override val name: String,
		override val size: Long,
		override val packageName: String,
		override val versionCode: Long,
		public val locale: Locale
	) : Apk(uri, name, size, packageName, versionCode, description = "") {

		override val description: String
			get() = locale.displayLanguage

		override fun isCompatible(context: Context): Boolean {
			return locale.language in deviceLocales(context).map { it.language }
		}
	}

	/**
	 * Unknown APK split.
	 */
	public data class Other(
		override val uri: Uri,
		override val name: String,
		override val size: Long,
		override val packageName: String,
		override val versionCode: Long
	) : Apk(uri, name, size, packageName, versionCode, description = name) {
		override fun isCompatible(context: Context): Boolean = true
	}

	/**
	 * Returns whether this APK split is compatible with the device.
	 */
	public abstract fun isCompatible(context: Context): Boolean

	public companion object {

		/**
		 * Reads provided [file], parses it and creates an [APK split][Apk] instance.
		 *
		 * Returns `null` if provided file is not an APK.
		 */
		@JvmStatic
		public fun fromFile(file: File, context: Context): Apk? {
			return fromFile(
				file,
				FileProvider.getUriForFile(context, "${context.packageName}.AckpineFileProvider", file)
			)
		}

		/**
		 * Reads file at provided [uri], parses it and creates an [APK split][Apk] instance.
		 *
		 * Returns `null` if provided file is not an APK.
		 */
		@JvmStatic
		public fun fromUri(uri: Uri, context: Context): Apk? {
			val file = uri.toFile(context)
			if (file.canRead()) {
				return fromFile(file, uri)
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
		internal fun fromZipEntry(zipPath: String, zipEntry: ZipEntry, inputStream: InputStream): Apk? {
			if (!zipEntry.isApk) {
				return null
			}
			val uri = ZippedFileProvider.getUriForZipEntry(zipPath, zipEntry.name)
			val name = zipEntry.name.substringAfterLast('/').substringBeforeLast('.')
			return createApkSplit(inputStream, name, uri, zipEntry.size)
		}

		private fun fromFile(file: File, uri: Uri): Apk? {
			if (!file.isApk) {
				return null
			}
			val name = file.name.substringAfterLast('/').substringBeforeLast('.')
			file.inputStream().use { inputStream ->
				return createApkSplit(inputStream, name, uri, file.length())
			}
		}

		private fun createApkSplit(inputStream: InputStream, name: String, uri: Uri, size: Long): Apk? {
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

		private fun configApkSplit(manifest: AndroidManifest, uri: Uri, size: Long): Apk {
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