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
import ru.solrudev.ackpine.AckpineFileProvider
import ru.solrudev.ackpine.ZippedFileProvider
import ru.solrudev.ackpine.helpers.entries
import ru.solrudev.ackpine.helpers.getFileFromUri
import ru.solrudev.ackpine.io.NonClosingInputStream.Companion.nonClosing
import ru.solrudev.ackpine.io.ZipEntryStream
import ru.solrudev.ackpine.io.toByteBuffer
import ru.solrudev.ackpine.splits.Dpi.Companion.dpi
import ru.solrudev.ackpine.splits.helpers.deviceLocales
import ru.solrudev.ackpine.splits.helpers.displayNameAndSize
import ru.solrudev.ackpine.splits.helpers.isApk
import ru.solrudev.ackpine.splits.helpers.localeFromSplitName
import ru.solrudev.ackpine.splits.parsing.ANDROID_MANIFEST_FILE_NAME
import ru.solrudev.ackpine.splits.parsing.AndroidManifest
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

/**
 * Represents an APK split.
 */
public sealed class Apk(

	/**
	 * [Uri] of the APK file.
	 */
	public open val uri: Uri,

	/**
	 * Split name of the APK.
	 */
	public open val name: String,

	/**
	 * Size of the APK file in bytes.
	 */
	public open val size: Long,

	/**
	 * Package name of the APK file.
	 */
	public open val packageName: String,

	/**
	 * An internal version number of the APK file.
	 */
	public open val versionCode: Long,

	/**
	 * Description of the APK split.
	 */
	public open val description: String
) {

	/**
	 * Marks an [Apk] as a configuration APK split ([Libs], [ScreenDensity] and [Localization]).
	 */
	public sealed interface ConfigSplit {

		/**
		 * Name of the split which this configuration APK is intended for.
		 *
		 * Empty value means this is for [Base] APK.
		 */
		public val configForSplit: String
	}

	/**
	 * Base APK.
	 */
	public data class Base(
		override val uri: Uri,
		override val name: String,
		override val size: Long,
		override val packageName: String,
		override val versionCode: Long,

		/**
		 * The version number shown to users.
		 */
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
	public data class Libs @JvmOverloads public constructor(
		override val uri: Uri,
		override val name: String,
		override val size: Long,
		override val packageName: String,
		override val versionCode: Long,

		/**
		 * An [Abi] of the native code contained in this APK file.
		 */
		public val abi: Abi,
		override val configForSplit: String = ""
	) : Apk(uri, name, size, packageName, versionCode, description = abi.name.lowercase()), ConfigSplit {

		override fun isCompatible(context: Context): Boolean = abi in Abi.deviceAbis

		/**
		 * @deprecated
		 */
		@Deprecated(message = "Binary compatibility", level = DeprecationLevel.HIDDEN)
		public fun copy(
			uri: Uri = this.uri,
			name: String = this.name,
			size: Long = this.size,
			packageName: String = this.packageName,
			versionCode: Long = this.versionCode,
			abi: Abi = this.abi
		): Libs = Libs(uri, name, size, packageName, versionCode, abi, configForSplit)
	}

	/**
	 * APK split containing graphic resources tailored to specific screen density.
	 */
	public data class ScreenDensity @JvmOverloads public constructor(
		override val uri: Uri,
		override val name: String,
		override val size: Long,
		override val packageName: String,
		override val versionCode: Long,

		/**
		 * A [Dpi] of the graphical resources contained in this APK file.
		 */
		public val dpi: Dpi,
		override val configForSplit: String = ""
	) : Apk(uri, name, size, packageName, versionCode, description = dpi.name.lowercase()), ConfigSplit {

		override fun isCompatible(context: Context): Boolean = dpi == context.dpi

		/**
		 * @deprecated
		 */
		@Deprecated(message = "Binary compatibility", level = DeprecationLevel.HIDDEN)
		public fun copy(
			uri: Uri = this.uri,
			name: String = this.name,
			size: Long = this.size,
			packageName: String = this.packageName,
			versionCode: Long = this.versionCode,
			dpi: Dpi = this.dpi
		): ScreenDensity = ScreenDensity(uri, name, size, packageName, versionCode, dpi, configForSplit)
	}

	/**
	 * APK split containing localized resources.
	 */
	public data class Localization @JvmOverloads public constructor(
		override val uri: Uri,
		override val name: String,
		override val size: Long,
		override val packageName: String,
		override val versionCode: Long,

		/**
		 * A [Locale] of the string resources contained in this APK file.
		 */
		public val locale: Locale,
		override val configForSplit: String = ""
	) : Apk(uri, name, size, packageName, versionCode, description = ""), ConfigSplit {

		override val description: String
			get() = locale.displayLanguage

		override fun isCompatible(context: Context): Boolean {
			return locale.language in deviceLocales(context).map { it.language }
		}

		/**
		 * @deprecated
		 */
		@Deprecated(message = "Binary compatibility", level = DeprecationLevel.HIDDEN)
		public fun copy(
			uri: Uri = this.uri,
			name: String = this.name,
			size: Long = this.size,
			packageName: String = this.packageName,
			versionCode: Long = this.versionCode,
			locale: Locale = this.locale
		): Localization = Localization(uri, name, size, packageName, versionCode, locale, configForSplit)
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
				FileProvider.getUriForFile(context, AckpineFileProvider.authority, file)
			)
		}

		/**
		 * Reads file at provided [uri], parses it and creates an [APK split][Apk] instance.
		 *
		 * Returns `null` if provided file is not an APK.
		 */
		@JvmStatic
		public fun fromUri(uri: Uri, context: Context): Apk? {
			val file = context.getFileFromUri(uri)
			if (file.canRead()) {
				return fromFile(file, uri)
			}
			if (!uri.isApk(context)) {
				return null
			}
			val (displayName, size) = uri.displayNameAndSize(context)
			val name = displayName.substringAfterLast('/').substringBeforeLast('.')
			val androidManifest = ZipEntryStream
				.open(uri, ANDROID_MANIFEST_FILE_NAME, context)
				?.use(InputStream::toByteBuffer) ?: return null
			return createApkSplit(androidManifest, name, uri, size)
		}

		@JvmSynthetic
		internal fun fromZipEntry(zipPath: String, zipEntry: ZipEntry, inputStream: InputStream): Apk? {
			if (!zipEntry.isApk) {
				return null
			}
			val uri = ZippedFileProvider.getUriForZipEntry(zipPath, zipEntry.name)
			val name = zipEntry.name.substringAfterLast('/').substringBeforeLast('.')
			val androidManifest = ZipInputStream(inputStream.nonClosing()).use { zipInputStream ->
				zipInputStream.entries().firstOrNull { it.name == ANDROID_MANIFEST_FILE_NAME } ?: return null
				zipInputStream.toByteBuffer()
			}
			return createApkSplit(androidManifest, name, uri, zipEntry.size)
		}

		private fun fromFile(file: File, uri: Uri): Apk? {
			if (!file.isApk) {
				return null
			}
			val name = file.name.substringAfterLast('/').substringBeforeLast('.')
			val androidManifest = ZipFile(file).use { zipFile ->
				val zipEntry = zipFile.getEntry(ANDROID_MANIFEST_FILE_NAME) ?: return null
				zipFile.getInputStream(zipEntry).toByteBuffer()
			}
			return createApkSplit(androidManifest, name, uri, file.length())
		}

		private fun createApkSplit(manifestByteBuffer: ByteBuffer, name: String, uri: Uri, size: Long): Apk? {
			val manifest = AndroidManifest(manifestByteBuffer) ?: return null
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
				dpi != null -> ScreenDensity(
					uri,
					splitName,
					size,
					manifest.packageName,
					manifest.versionCode,
					dpi,
					manifest.configForSplit
				)

				abi != null -> Libs(
					uri,
					splitName,
					size,
					manifest.packageName,
					manifest.versionCode,
					abi,
					manifest.configForSplit
				)

				locale != null -> Localization(
					uri,
					splitName,
					size,
					manifest.packageName,
					manifest.versionCode,
					locale,
					manifest.configForSplit
				)

				else -> Other(
					uri,
					splitName,
					size,
					manifest.packageName,
					manifest.versionCode
				)
			}
		}
	}
}