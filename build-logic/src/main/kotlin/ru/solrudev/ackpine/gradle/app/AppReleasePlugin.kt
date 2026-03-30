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
import com.android.build.api.dsl.SigningConfig
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.ApplicationVariant
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.problems.ProblemGroup
import org.gradle.api.problems.ProblemId
import org.gradle.api.problems.Problems
import org.gradle.api.problems.Severity
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.the
import ru.solrudev.ackpine.gradle.helpers.addOutgoingArtifact
import ru.solrudev.ackpine.gradle.helpers.getOrThrow
import ru.solrudev.ackpine.gradle.helpers.libraryElements
import ru.solrudev.ackpine.gradle.helpers.propertiesProvider
import ru.solrudev.ackpine.gradle.helpers.withReleaseBuildType
import ru.solrudev.ackpine.gradle.tasks.ExtractBundleApksTask
import ru.solrudev.ackpine.gradle.tasks.PrepareSingleApkTask
import java.io.File
import javax.inject.Inject
import kotlin.jvm.optionals.getOrNull

private const val APP_SIGNING_KEY_ALIAS = "APP_SIGNING_KEY_ALIAS"
private const val APP_SIGNING_KEY_PASSWORD = "APP_SIGNING_KEY_PASSWORD"
private const val APP_SIGNING_KEY_STORE_PASSWORD = "APP_SIGNING_KEY_STORE_PASSWORD"
private const val APP_SIGNING_KEY_STORE_PATH = "APP_SIGNING_KEY_STORE_PATH"
private const val APP_SIGNING_PREFIX = "APP_SIGNING_"
private val PROBLEM_GROUP = ProblemGroup.create("ackpine-app-release", "Ackpine AppRelease plugin")

public class AppReleasePlugin @Inject public constructor(private val problems: Problems) : Plugin<Project> {

	override fun apply(target: Project): Unit = target.run {
		pluginManager.withPlugin("com.android.application") {
			val extension = extensions.create<AppReleaseExtension>("app")
			extension.publishSplits.convention(false)
			val signingConfig = configureSigning()
			registerTasks(extension, signingConfig)
		}
	}

	private fun Project.configureSigning(): Provider<out SigningConfig> {
		val android = the<ApplicationExtension>()
		val keystorePropertiesFile = layout.settingsDirectory.file("keystore.properties")
		val fileConfigProvider = propertiesProvider(name = "app_signing", keystorePropertiesFile)
			.map { properties ->
				properties.filterKeys { it.startsWith(APP_SIGNING_PREFIX) }
			}
		val environmentConfigProvider = providers.environmentVariablesPrefixedBy(APP_SIGNING_PREFIX)
		val releaseSigningConfig = android.releaseSigningConfigProvider(fileConfigProvider, environmentConfigProvider)
		android.buildTypes.named("release") {
			signingConfig = releaseSigningConfig.get()
		}
		return releaseSigningConfig
	}

	@Suppress("NewApi")
	private fun Project.registerTasks(extension: AppReleaseExtension, signingConfig: Provider<out SigningConfig>) {
		val appElements = lazy {
			configurations.consumable("ackpineAppElements") {
				libraryElements(LIBRARY_ELEMENTS)
			}
		}
		val apkElements = configurations.consumable(APK_CONFIGURATION_NAME)
		configureBundles(extension, appElements, signingConfig)
		extensions.configure<ApplicationAndroidComponentsExtension> {
			onVariants(withReleaseBuildType()) { variant ->
				if (!extension.publishSplits.get()) {
					val produceArtifactsTask = registerProduceArtifactsTaskForVariant(variant)
					appElements.value.addOutgoingArtifact(produceArtifactsTask)
				}
				val prepareApk = registerPrepareApkFileTaskForVariant(variant)
				apkElements.addOutgoingArtifact(prepareApk)
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

	private fun Project.registerProduceArtifactsTaskForVariant(variant: ApplicationVariant): TaskProvider<Sync> {
		val outputDir = layout.buildDirectory.dir("generated/${variant.name}_artifacts")
		val apks = variant.artifacts.get(SingleArtifact.APK)
		val mapping = if (variant.isMinifyEnabled) {
			variant.artifacts.get(SingleArtifact.OBFUSCATION_MAPPING_FILE)
		} else {
			null
		}
		val mappingDestinationName = "mapping-${project.name}-${variant.name}.txt"
		val taskName = variant.computeTaskName(action = "produce", subject = "artifacts")
		return tasks.register<Sync>(taskName) {
			from(apks) {
				include("*.apk")
			}
			if (mapping != null) {
				from(mapping) {
					rename { mappingDestinationName }
				}
			}
			into(outputDir)
		}
	}

	@Suppress("NewApi")
	private fun Project.configureBundles(
		extension: AppReleaseExtension,
		appElements: Lazy<NamedDomainObjectProvider<out Configuration>>,
		signingConfig: Provider<out SigningConfig>
	) = extensions.configure<ApplicationAndroidComponentsExtension> {
		finalizeDsl {
			if (!extension.publishSplits.get()) {
				return@finalizeDsl
			}
			val bundletool = extensions.findByType<VersionCatalogsExtension>()
				?.named("libs")
				?.findLibrary("bundletool")
				?.getOrNull()
				?: return@finalizeDsl reportBundletoolNotFound()
			val bundletoolScope = configurations.dependencyScope("bundletool")
			val bundletoolClasspath = configurations.resolvable("bundletoolClasspath") {
				extendsFrom(bundletoolScope)
			}
			dependencies.add(bundletoolScope.name, bundletool)
			onVariants(withReleaseBuildType()) { variant ->
				val extractTask = registerExtractBundleApksTaskForVariant(variant, bundletoolClasspath, signingConfig)
				appElements.value.addOutgoingArtifact(extractTask)
			}
		}
	}

	private fun Project.registerExtractBundleApksTaskForVariant(
		variant: ApplicationVariant,
		bundletoolClasspath: Provider<*>,
		signingConfig: Provider<out SigningConfig>
	): TaskProvider<ExtractBundleApksTask> {
		val bundle = variant.artifacts.get(SingleArtifact.BUNDLE)
		val aapt2 = the<ApplicationAndroidComponentsExtension>().sdkComponents.aapt2
		val outputDir = layout.buildDirectory.dir("generated/bundle_apks/${variant.name}")
		val taskName = variant.computeTaskName(action = "extract", subject = "bundleApks")
		return tasks.register<ExtractBundleApksTask>(taskName) {
			bundleFile = bundle
			this.bundletoolClasspath.from(bundletoolClasspath)
			this.aapt2 = aapt2
			keystoreFile.fileProvider(signingConfig.map { it.storeFile!! })
			keyAlias = signingConfig.map { it.keyAlias!! }
			keyPassword = signingConfig.map { it.keyPassword!! }
			storePassword = signingConfig.map { it.storePassword!! }
			artifactDirectoryName = project.name
			outputDirectory = outputDir
		}
	}

	private fun Project.registerPrepareApkFileTaskForVariant(
		variant: ApplicationVariant
	): TaskProvider<PrepareSingleApkTask> {
		val apk = variant.artifacts.get(SingleArtifact.APK)
		val artifactsLoader = variant.artifacts.getBuiltArtifactsLoader()
		val outputFile = layout.buildDirectory.file("generated/${variant.name}_apk/${project.name}-${variant.name}.apk")
		val taskName = variant.computeTaskName(action = "prepare", subject = "apkFile")
		return tasks.register<PrepareSingleApkTask>(taskName) {
			apkDirectory = apk
			builtArtifactsLoader = artifactsLoader
			outputApk = outputFile
		}
	}

	private fun reportBundletoolNotFound() {
		val problem = ProblemId.create(
			"bundletool-not-found",
			"'bundletool' version catalog entry was not found.",
			PROBLEM_GROUP
		)
		problems.reporter.report(problem) {
			details(
				"""
				|'bundletool' version catalog entry was not found in libs.versions.toml.
				|Split APKs won't be published.
			""".trimMargin()
			)
			solution("Add 'com.android.tools.build:bundletool' to 'libs' version catalog.")
			severity(Severity.WARNING)
		}
	}

	internal companion object {
		internal const val LIBRARY_ELEMENTS = "appArtifacts"
		internal const val APK_CONFIGURATION_NAME = "ackpineApkElements"
	}
}