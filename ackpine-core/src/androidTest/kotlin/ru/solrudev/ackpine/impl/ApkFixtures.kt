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

package ru.solrudev.ackpine.impl

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.test.platform.app.InstrumentationRegistry
import ru.solrudev.ackpine.AckpineFileProvider
import java.io.File

object ApkFixtures {

	const val PACKAGE_NAME = "ru.solrudev.ackpine.core.test"
	const val FIXTURE_PACKAGE_NAME = "ru.solrudev.ackpine.sample.fixture"
	const val INSTALLER_PACKAGE_NAME = "ru.solrudev.ackpine.sample.app"
	const val INSTALLER_LABEL = "Ackpine Installer App"
	private const val APK_V1 = "apk-fixture-v1-release.apk"
	private const val APK_MIN_SDK = "apk-fixture-minSdk-release.apk"
	private const val APK_INSTALLER = "installer-app-release.apk"

	private val context: Context
		get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext

	fun fixtureUri() = getUri(APK_V1)
	fun installerAppUri() = getUri(APK_INSTALLER)
	fun highMinSdkUri() = getUri(APK_MIN_SDK)

	private fun getUri(fileName: String): Uri {
		val uri = FileProvider.getUriForFile(context, AckpineFileProvider.Companion.authority, getApkFile(fileName))
		context.grantUriPermission(INSTALLER_PACKAGE_NAME, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
		return uri
	}

	private fun getApkFile(assetName: String): File {
		val targetDir = File(context.cacheDir, "ackpine-tests")
		if (!targetDir.exists()) {
			targetDir.mkdirs()
		}
		val target = File(targetDir, assetName)
		if (target.exists()) {
			return target
		}
		context.assets.open(assetName).use { input ->
			target.outputStream().use { output -> input.copyTo(output) }
		}
		return target
	}
}