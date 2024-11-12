/*
 * Copyright (C) 2024 Ilya Fomichev
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

package ru.solrudev.ackpine.gradle

import com.android.build.api.dsl.ApkSigningConfig
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.gradle.AppPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.hasPlugin
import ru.solrudev.ackpine.gradle.helpers.toProperties
import java.io.File

public class AppReleasePlugin : Plugin<Project> {

	override fun apply(target: Project): Unit = target.run {
		check(plugins.hasPlugin(AppPlugin::class)) {
			"Applying app-release plugin requires the Android application plugin to be applied"
		}
		configureSigning()
	}

	private fun Project.configureSigning() = extensions.configure<ApplicationExtension> {
		val releaseSigningConfig = releaseSigningConfigProvider(rootProject)
		buildTypes.named("release") {
			signingConfig = releaseSigningConfig.get()
		}
	}

	private fun ApplicationExtension.releaseSigningConfigProvider(
		rootProject: Project
	) = signingConfigs.register("releaseSigningConfig") {
		initWith(signingConfigs["debug"])
		val keystorePropertiesFile = rootProject.file("keystore.properties")
		if (keystorePropertiesFile.exists()) {
			val properties = keystorePropertiesFile.toProperties()
			readSigningConfig { key -> properties[key] as? String }
		} else {
			readSigningConfig { key -> System.getenv(key) }
		}
		enableV3Signing = true
	}

	private inline fun ApkSigningConfig.readSigningConfig(valueProvider: (key: String) -> String?) {
		keyAlias = getOrThrow(
			key = "APP_SIGNING_KEY_ALIAS",
			name = "Signing key alias",
			valueProvider
		)
		keyPassword = getOrThrow(
			key = "APP_SIGNING_KEY_PASSWORD",
			name = "Signing key password",
			valueProvider
		)
		storePassword = getOrThrow(
			key = "APP_SIGNING_KEY_STORE_PASSWORD",
			name = "Signing key store password",
			valueProvider
		)
		storeFile = getOrThrow(
			key = "APP_SIGNING_KEY_STORE_PATH",
			name = "Signing key store path",
			valueProvider
		).let(::File)
	}

	private inline fun getOrThrow(key: String, name: String, valueProvider: (key: String) -> String?): String {
		val value = valueProvider(key)
		check(!value.isNullOrEmpty()) { "$name was not provided" }
		return value
	}
}