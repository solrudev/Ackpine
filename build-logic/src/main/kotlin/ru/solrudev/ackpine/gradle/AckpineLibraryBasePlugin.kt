/*
 * Copyright (C) 2025 Ilya Fomichev
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
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.android.build.gradle.LibraryPlugin
import kotlinx.validation.ApiValidationExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.the
import ru.solrudev.ackpine.gradle.helpers.addOutgoingArtifact
import ru.solrudev.ackpine.gradle.helpers.libraryElements
import ru.solrudev.ackpine.gradle.helpers.withReleaseBuildType
import ru.solrudev.ackpine.gradle.versioning.ackpineVersion

public class AckpineLibraryBasePlugin : Plugin<Project> {

	override fun apply(target: Project): Unit = target.run {
		group = Constants.PACKAGE_NAME
		version = ackpineVersion.get().toString()
		pluginManager.run {
			apply(LibraryPlugin::class)
		}
		configureJava()
		val libraryExtension = the<LibraryExtension>()
		val apiValidationExtension = extensions.findByType<ApiValidationExtension>()?.apply {
			nonPublicMarkers += "androidx.annotation.RestrictTo"
		}
		extensions.create("ackpine", AckpineLibraryExtension::class.java, libraryExtension, apiValidationExtension)
		configureAndroid()
		registerConsumableLibraryConfiguration()
	}

	private fun Project.configureJava() = extensions.configure<JavaPluginExtension> {
		toolchain.languageVersion = JavaLanguageVersion.of(Constants.JDK_VERSION)
	}

	private fun Project.configureAndroid() = extensions.configure<LibraryExtension> {
		compileSdk = Constants.COMPILE_SDK
		buildToolsVersion = Constants.BUILD_TOOLS_VERSION

		defaultConfig {
			minSdk = Constants.MIN_SDK
			consumerProguardFiles("consumer-rules.pro")
		}

		buildTypes.named("release") {
			isMinifyEnabled = false
			proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
		}

		compileOptions {
			sourceCompatibility = JavaVersion.VERSION_1_8
			targetCompatibility = JavaVersion.VERSION_1_8
		}
	}

	private fun Project.registerConsumableLibraryConfiguration() {
		val libraryElements = configurations.consumable("ackpineLibraryElements") {
			libraryElements(objects.named(LIBRARY_ELEMENTS))
		}
		extensions.configure<LibraryAndroidComponentsExtension> {
			onVariants(withReleaseBuildType()) { variant ->
				val aar = variant.artifacts.get(SingleArtifact.AAR)
				libraryElements.addOutgoingArtifact(aar)
			}
		}
	}

	internal companion object {
		internal const val LIBRARY_ELEMENTS = "aar"
		internal const val PLUGIN_ID = "ru.solrudev.ackpine.library.base"
	}
}