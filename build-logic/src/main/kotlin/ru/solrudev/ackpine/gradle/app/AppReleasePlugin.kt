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

package ru.solrudev.ackpine.gradle.app

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.Variant
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import ru.solrudev.ackpine.gradle.helpers.addOutgoingArtifact
import ru.solrudev.ackpine.gradle.helpers.getOrThrow
import ru.solrudev.ackpine.gradle.helpers.libraryElements
import ru.solrudev.ackpine.gradle.helpers.propertiesProvider
import ru.solrudev.ackpine.gradle.helpers.withReleaseBuildType
import java.io.File

private const val APP_SIGNING_KEY_ALIAS = "APP_SIGNING_KEY_ALIAS"
private const val APP_SIGNING_KEY_PASSWORD = "APP_SIGNING_KEY_PASSWORD"
private const val APP_SIGNING_KEY_STORE_PASSWORD = "APP_SIGNING_KEY_STORE_PASSWORD"
private const val APP_SIGNING_KEY_STORE_PATH = "APP_SIGNING_KEY_STORE_PATH"
private const val APP_SIGNING_PREFIX = "APP_SIGNING_"

public class AppReleasePlugin : Plugin<Project> {

	override fun apply(target: Project): Unit = target.run {
		pluginManager.withPlugin("com.android.application") {
			configureSigning()
			registerProduceReleaseArtifactsTasks()
		}
	}

	private fun Project.configureSigning() = extensions.configure<ApplicationExtension> {
		val keystorePropertiesFile = layout.settingsDirectory.file("keystore.properties")
		val fileConfigProvider = propertiesProvider(name = "app_signing", keystorePropertiesFile)
			.map { properties ->
				properties.filterKeys { it.startsWith(APP_SIGNING_PREFIX) }
			}
		val environmentConfigProvider = providers.environmentVariablesPrefixedBy(APP_SIGNING_PREFIX)
		val releaseSigningConfig = releaseSigningConfigProvider(fileConfigProvider, environmentConfigProvider)
		buildTypes.named("release") {
			signingConfig = releaseSigningConfig.get()
		}
	}

	private fun Project.registerProduceReleaseArtifactsTasks() {
		extensions.configure<ApplicationAndroidComponentsExtension> {
			val sampleElements = configurations.consumable("ackpineSampleElements") {
				libraryElements(objects.named(LIBRARY_ELEMENTS))
			}
			onVariants(withReleaseBuildType()) { variant ->
				val produceArtifactsTask = registerProduceArtifactsTaskForVariant(variant)
				sampleElements.addOutgoingArtifact(produceArtifactsTask)
				configureCleanTask(produceArtifactsTask)
			}
		}
	}

	private fun ApplicationExtension.releaseSigningConfigProvider(
		fileConfigProvider: Provider<Map<String, String>>,
		environmentConfigProvider: Provider<Map<String, String>>
	) = signingConfigs.register("release") {
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

	private fun Project.registerProduceArtifactsTaskForVariant(variant: Variant): TaskProvider<*> {
		val releaseDir = layout.projectDirectory.dir(variant.name)
		val apks = variant.artifacts.get(SingleArtifact.APK)
		val mapping = variant.artifacts.get(SingleArtifact.OBFUSCATION_MAPPING_FILE)
		val mappingDestinationName = "mapping-${project.name}-${variant.name}.txt"
		val taskName = variant.computeTaskName(action = "produce", subject = "artifacts")
		return tasks.register<Sync>(taskName) {
			from(apks) {
				include("*.apk")
			}
			from(mapping) {
				rename { mappingDestinationName }
			}
			into(releaseDir)
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