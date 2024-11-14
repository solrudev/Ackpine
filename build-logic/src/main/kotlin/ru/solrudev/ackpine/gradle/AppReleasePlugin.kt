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

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.Variant
import com.android.build.gradle.AppPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.hasPlugin
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import ru.solrudev.ackpine.gradle.helpers.getOrThrow
import ru.solrudev.ackpine.gradle.helpers.toPropertiesMap
import ru.solrudev.ackpine.gradle.helpers.withReleaseBuildType
import ru.solrudev.ackpine.gradle.tasks.BuildReleaseSamplesTask
import java.io.File

private const val APP_SIGNING_KEY_ALIAS = "APP_SIGNING_KEY_ALIAS"
private const val APP_SIGNING_KEY_PASSWORD = "APP_SIGNING_KEY_PASSWORD"
private const val APP_SIGNING_KEY_STORE_PASSWORD = "APP_SIGNING_KEY_STORE_PASSWORD"
private const val APP_SIGNING_KEY_STORE_PATH = "APP_SIGNING_KEY_STORE_PATH"

private val signingConfigKeys = setOf(
	APP_SIGNING_KEY_ALIAS,
	APP_SIGNING_KEY_PASSWORD,
	APP_SIGNING_KEY_STORE_PASSWORD,
	APP_SIGNING_KEY_STORE_PATH
)

public class AppReleasePlugin : Plugin<Project> {

	override fun apply(target: Project): Unit = target.run {
		check(plugins.hasPlugin(AppPlugin::class)) {
			"Applying app-release plugin requires the Android application plugin to be applied"
		}
		configureSigning()
		registerCopyReleaseArtifactsTasks()
	}

	private fun Project.configureSigning() = extensions.configure<ApplicationExtension> {
		val releaseSigningConfig = releaseSigningConfigProvider(rootProject.file("keystore.properties"))
		buildTypes.named("release") {
			signingConfig = releaseSigningConfig.get()
		}
	}

	private fun Project.registerCopyReleaseArtifactsTasks() {
		extensions.configure<ApplicationAndroidComponentsExtension> {
			onVariants(withReleaseBuildType()) { variant ->
				registerCopyArtifactsTaskForVariant(variant)
			}
		}
	}

	private fun ApplicationExtension.releaseSigningConfigProvider(
		keystorePropertiesFile: File
	) = signingConfigs.register("releaseSigningConfig") {
		initWith(signingConfigs["debug"])
		val config = if (keystorePropertiesFile.exists()) {
			keystorePropertiesFile.toPropertiesMap()
		} else {
			System.getenv()
		}
		if (signingConfigKeys.any { it in config.keys }) {
			keyAlias = config.getOrThrow(APP_SIGNING_KEY_ALIAS)
			keyPassword = config.getOrThrow(APP_SIGNING_KEY_PASSWORD)
			storePassword = config.getOrThrow(APP_SIGNING_KEY_STORE_PASSWORD)
			storeFile = config.getOrThrow(APP_SIGNING_KEY_STORE_PATH).let(::File)
		}
		enableV3Signing = true
	}

	private fun Project.registerCopyArtifactsTaskForVariant(variant: Variant) {
		val releaseDir = rootProject.layout.projectDirectory.dir("release")
		val apks = variant.artifacts.get(SingleArtifact.APK).map { directory ->
			directory.asFileTree.matching { include("*.apk") }
		}
		val mapping = variant.artifacts.get(SingleArtifact.OBFUSCATION_MAPPING_FILE)
		val variantName = variant.name
		val projectName = project.name
		val taskName = variant.computeTaskName(action = "copy", subject = "artifacts")
		val copyArtifacts = tasks.register<Copy>(taskName) {
			from(apks, mapping)
			rename { path ->
				path.replace(mapping.get().asFile.name, "mapping-$projectName-$variantName.txt")
			}
			into(releaseDir)
		}
		rootProject.tasks.withType<BuildReleaseSamplesTask>().configureEach {
			outputDir = releaseDir
			dependsOn(copyArtifacts)
		}
	}
}