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

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.internal.dsl.SigningConfig
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.hasPlugin
import ru.solrudev.ackpine.gradle.helpers.toProperties
import java.io.File
import java.util.Properties

public class AppReleaseSigningPlugin : Plugin<Project> {

	override fun apply(target: Project): Unit = target.run {
		check(plugins.hasPlugin(AppPlugin::class)) {
			"Applying app-release-signing plugin requires the Android application plugin to be applied"
		}
		configureSigning()
	}

	private fun Project.configureSigning() = extensions.configure<AppExtension> {
		val releaseSigningConfig = releaseSigningConfigProvider(rootProject)
		buildTypes
			.matching { it.name.lowercase().endsWith("release") }
			.configureEach {
				signingConfig = releaseSigningConfig.get()
			}
	}

	private fun AppExtension.releaseSigningConfigProvider(
		rootProject: Project
	) = signingConfigs.register("releaseSigningConfig") {
		initWith(signingConfigs["debug"])
		val keystorePropertiesFile = rootProject.file("keystore.properties")
		if (keystorePropertiesFile.exists()) {
			readSigningConfig(keystorePropertiesFile.toProperties()::getOrThrow)
		} else {
			readSigningConfig(::getEnvOrThrow)
		}
		enableV3Signing = true
	}

	private inline fun SigningConfig.readSigningConfig(valueProvider: (key: String, name: String) -> String) {
		keyAlias = valueProvider("APP_SIGNING_KEY_ALIAS", "Signing key alias")
		keyPassword = valueProvider("APP_SIGNING_KEY_PASSWORD", "Signing key password")
		storeFile = valueProvider("APP_SIGNING_KEY_STORE_PATH", "Signing key store path").let(::File)
		storePassword = valueProvider("APP_SIGNING_KEY_STORE_PASSWORD", "Signing key store password")
	}

}

private fun Properties.getOrThrow(key: String, name: String): String {
	val value = get(key) as? String
	check(!value.isNullOrEmpty()) { "$name was not provided" }
	return value
}

private fun getEnvOrThrow(key: String, name: String): String {
	val value = System.getenv(key)
	check(!value.isNullOrEmpty()) { "$name was not provided" }
	return value
}