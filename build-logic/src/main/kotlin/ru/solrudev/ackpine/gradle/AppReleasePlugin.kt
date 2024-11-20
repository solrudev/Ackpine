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
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.hasPlugin
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import ru.solrudev.ackpine.gradle.helpers.consumable
import ru.solrudev.ackpine.gradle.helpers.getOrThrow
import ru.solrudev.ackpine.gradle.helpers.properties
import ru.solrudev.ackpine.gradle.helpers.withReleaseBuildType
import java.io.File

private const val APP_SIGNING_KEY_ALIAS = "APP_SIGNING_KEY_ALIAS"
private const val APP_SIGNING_KEY_PASSWORD = "APP_SIGNING_KEY_PASSWORD"
private const val APP_SIGNING_KEY_STORE_PASSWORD = "APP_SIGNING_KEY_STORE_PASSWORD"
private const val APP_SIGNING_KEY_STORE_PATH = "APP_SIGNING_KEY_STORE_PATH"
private const val APP_SIGNING_PREFIX = "APP_SIGNING_"

public class AppReleasePlugin : Plugin<Project> {

	override fun apply(target: Project): Unit = target.run {
		check(plugins.hasPlugin(AppPlugin::class)) {
			"Applying app-release plugin requires the Android application plugin to be applied"
		}
		configureSigning()
		registerCopyReleaseArtifactsTasks()
	}

	private fun Project.configureSigning() = extensions.configure<ApplicationExtension> {
		val keystoreConfigFile = isolated.rootProject.projectDirectory.file("keystore.properties")
		val fileConfigProvider = providers.properties(keystoreConfigFile).map { properties ->
			properties.filterKeys { it.startsWith(APP_SIGNING_PREFIX) }
		}
		val environmentConfigProvider = providers.environmentVariablesPrefixedBy(APP_SIGNING_PREFIX)
		val releaseSigningConfig = releaseSigningConfigProvider(fileConfigProvider, environmentConfigProvider)
		buildTypes.named("release") {
			signingConfig = releaseSigningConfig.get()
		}
	}

	private fun Project.registerCopyReleaseArtifactsTasks() {
		extensions.configure<ApplicationAndroidComponentsExtension> {
			val appConfiguration = createConsumableAppConfiguration()
			onVariants(withReleaseBuildType()) { variant ->
				val copyArtifactsTask = registerCopyArtifactsTaskForVariant(variant)
				appConfiguration.addOutgoingArtifact(copyArtifactsTask)
				configureCleanTask(copyArtifactsTask)
			}
		}
	}

	private fun ApplicationExtension.releaseSigningConfigProvider(
		fileConfigProvider: Provider<Map<String, String>>,
		environmentConfigProvider: Provider<Map<String, String>>
	) = signingConfigs.register("releaseSigningConfig") {
		initWith(signingConfigs["debug"])
		val config = fileConfigProvider.get().ifEmpty { environmentConfigProvider.get() }
		if (config.isNotEmpty()) {
			keyAlias = config.getOrThrow(APP_SIGNING_KEY_ALIAS)
			keyPassword = config.getOrThrow(APP_SIGNING_KEY_PASSWORD)
			storePassword = config.getOrThrow(APP_SIGNING_KEY_STORE_PASSWORD)
			storeFile = config.getOrThrow(APP_SIGNING_KEY_STORE_PATH).let(::File)
		}
		enableV3Signing = true
	}

	private fun Project.registerCopyArtifactsTaskForVariant(variant: Variant): TaskProvider<*> {
		val releaseDir = isolated.rootProject.projectDirectory.dir("release")
		val apks = variant.artifacts.get(SingleArtifact.APK).map { directory ->
			directory.asFileTree.matching { include("*.apk") }
		}
		val mapping = variant.artifacts.get(SingleArtifact.OBFUSCATION_MAPPING_FILE)
		val variantName = variant.name
		val projectName = project.name
		val taskName = variant.computeTaskName(action = "copy", subject = "artifacts")
		return tasks.register<Copy>(taskName) {
			from(apks, mapping)
			rename { path ->
				path.replace(mapping.get().asFile.name, "mapping-$projectName-$variantName.txt")
			}
			into(releaseDir)
		}
	}

	private fun Project.createConsumableAppConfiguration(): NamedDomainObjectProvider<Configuration> {
		return configurations.register("app") {
			consumable()
			attributes {
				attribute(LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LIBRARY_ELEMENTS))
			}
		}
	}

	private fun NamedDomainObjectProvider<Configuration>.addOutgoingArtifact(artifact: Any) {
		configure {
			outgoing.artifact(artifact)
		}
	}

	private fun Project.configureCleanTask(deleteTarget: Any) {
		tasks.named<Delete>("clean") {
			delete(deleteTarget)
		}
	}

	internal companion object {
		internal const val LIBRARY_ELEMENTS = "appArtifacts"
	}
}