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

package ru.solrudev.ackpine.gradle.validation

import kotlinx.validation.ApiValidationExtension
import kotlinx.validation.BinaryCompatibilityValidatorPlugin
import kotlinx.validation.KotlinApiBuildTask
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidExtension
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import ru.solrudev.ackpine.gradle.AckpineLibraryBasePlugin.Companion.ABI_CHECK_CONFIGURATION
import ru.solrudev.ackpine.gradle.AckpineLibraryBasePlugin.Companion.ABI_UPDATE_CONFIGURATION
import ru.solrudev.ackpine.gradle.helpers.abiValidation

internal object AbiValidationSupport {

	fun enable(project: Project) = when (AbiValidation.resolve(project)) {
		AbiValidation.BCV -> configureBcv(project)
		AbiValidation.KGP -> configureKgp(project)
	}

	// Workaround for built-in Kotlin as per https://github.com/embrace-io/embrace-android-sdk/pull/3103
	@Suppress("NewApi")
	private fun configureBcv(project: Project): Unit = project.run {
		pluginManager.apply(BinaryCompatibilityValidatorPlugin::class.java)
		pluginManager.apply("kotlin-android")
		extensions.configure<ApiValidationExtension> {
			nonPublicMarkers += "androidx.annotation.RestrictTo"
		}
		val kotlinVersion = extensions.getByType<VersionCatalogsExtension>()
			.named("libs")
			.findVersion("kotlin")
			.get()
			.requiredVersion
		val kotlinMetadata = configurations.dependencyScope("kotlinMetadata")
		val kotlinMetadataForBcv = configurations.resolvable("kotlinMetadataForBcv") {
			extendsFrom(kotlinMetadata)
		}
		dependencies.add(
			kotlinMetadata.name,
			"org.jetbrains.kotlin:kotlin-metadata-jvm:$kotlinVersion"
		)
		tasks.withType<KotlinApiBuildTask>().configureEach {
			val compileTask = tasks.named<KotlinJvmCompile>("compileReleaseKotlin")
			inputClassesDirs.from(compileTask)
			runtimeClasspath.from(kotlinMetadataForBcv)
		}
		configurations.named(ABI_UPDATE_CONFIGURATION) {
			outgoing.artifact(layout.projectDirectory.dir("api")) {
				builtBy("apiDump")
			}
		}
		configurations.named(ABI_CHECK_CONFIGURATION) {
			outgoing.artifact(layout.buildDirectory.dir("abiCheck")) {
				builtBy("apiCheck")
			}
		}
	}

	@OptIn(ExperimentalAbiValidation::class)
	private fun configureKgp(project: Project): Unit = project.run {
		val abiValidation = extensions
			.getByType<KotlinAndroidExtension>()
			.abiValidation
			.apply {
				enabled = true
				filters.exclude.annotatedWith.add("androidx.annotation.RestrictTo")
			}
		configurations.named(ABI_UPDATE_CONFIGURATION) {
			outgoing.artifact(abiValidation.legacyDump.legacyUpdateTaskProvider)
		}
		configurations.named(ABI_CHECK_CONFIGURATION) {
			outgoing.artifact(layout.buildDirectory.dir("abiCheck")) {
				builtBy(abiValidation.legacyDump.legacyCheckTaskProvider)
			}
		}
	}
}