/*
 * Copyright (C) 2026 Ilya Fomichev
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

package ru.solrudev.ackpine.splits.testutil

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import ru.solrudev.ackpine.splits.helpers.isApk
import java.io.File
import java.util.zip.ZipFile

object SplitFixtures {

	const val PACKAGE_NAME = "ru.solrudev.ackpine.sample.splits.fixture"
	const val VERSION_CODE = 1L
	const val VERSION_NAME = "1.0"
	const val FEATURE_NAME = "dynamic_feature_fixture"
	const val ASSET_PACK_NAME = "texture_asset_pack_fixture"
	const val APKS_ARCHIVE = "bundle.apks"
	const val BASE_APK = "base-master.apk"
	const val FEATURE_APK = "$FEATURE_NAME-master.apk"
	const val ARM64_V8A_APK = "base-arm64_v8a.apk"
	const val ARMEABI_V7A_APK = "base-armeabi_v7a.apk"
	const val LOCALE_APK = "base-en.apk"
	const val HDPI_APK = "base-hdpi.apk"
	const val LDPI_APK = "base-ldpi.apk"
	const val MDPI_APK = "base-mdpi.apk"
	const val TVDPI_APK = "base-tvdpi.apk"
	const val XHDPI_APK = "base-xhdpi.apk"
	const val XXHDPI_APK = "base-xxhdpi.apk"
	const val XXXHDPI_APK = "base-xxxhdpi.apk"
	const val FEATURE_LOCALE_APK = "$FEATURE_NAME-en.apk"
	const val FEATURE_HDPI_APK = "$FEATURE_NAME-hdpi.apk"
	const val FEATURE_LDPI_APK = "$FEATURE_NAME-ldpi.apk"
	const val FEATURE_MDPI_APK = "$FEATURE_NAME-mdpi.apk"
	const val FEATURE_TVDPI_APK = "$FEATURE_NAME-tvdpi.apk"
	const val FEATURE_XHDPI_APK = "$FEATURE_NAME-xhdpi.apk"
	const val FEATURE_XXHDPI_APK = "$FEATURE_NAME-xxhdpi.apk"
	const val FEATURE_XXXHDPI_APK = "$FEATURE_NAME-xxxhdpi.apk"
	const val ASSET_PACK_MASTER_APK = "$ASSET_PACK_NAME-master.apk"
	const val ASSET_PACK_ASTC_APK = "$ASSET_PACK_NAME-astc.apk"
	const val ASSET_PACK_FALLBACK_APK = "$ASSET_PACK_NAME-other_tcf.apk"
	private const val FIXTURE_DIRECTORY = "splits-fixture"

	private val context: Context
		get() = ApplicationProvider.getApplicationContext()

	private val fixturesDir: File
		get() = File(context.cacheDir, "ackpine-splits-tests").also { dir ->
			dir.mkdirs()
			copyAssets(dir)
		}

	fun apkFile(name: String) = File(fixturesDir, name)
	fun apkFileUri(name: String): Uri = Uri.fromFile(apkFile(name))
	fun apksArchive() = File(fixturesDir, APKS_ARCHIVE)

	fun firstApkEntryMetadata() = ZipFile(apksArchive()).use { zip ->
		val entry = zip.entries().asSequence().first { it.isApk }
		ApkEntryMetadata(entry.name, entry.size)
	}

	fun firstApkEntry() = ZipFile(apksArchive()).use { zip ->
		val entry = zip.entries().asSequence().first { it.isApk }
		ApkEntry(
			ApkEntryMetadata(entry.name, entry.size),
			zip.getInputStream(entry).use { it.readBytes() }
		)
	}

	data class ApkEntryMetadata(val name: String, val size: Long)
	class ApkEntry(val metadata: ApkEntryMetadata, val bytes: ByteArray)

	private fun copyAssets(outputDir: File) {
		val assets = context.assets
		val fileNames = assets.list(FIXTURE_DIRECTORY) ?: return
		for (fileName in fileNames) {
			if (!fileName.endsWith(".apk") && !fileName.endsWith(".apks")) {
				continue
			}
			val file = File(outputDir, fileName)
			if (!file.exists()) {
				assets.open("$FIXTURE_DIRECTORY/$fileName").use { input ->
					file.outputStream().use { output -> input.copyTo(output) }
				}
			}
		}
	}
}