/*
 * Copyright (C) 2023-2024 Ilya Fomichev
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
import kotlinx.validation.BinaryCompatibilityValidatorPlugin
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinAndroidPluginWrapper
import ru.solrudev.ackpine.gradle.helpers.consumable
import ru.solrudev.ackpine.gradle.helpers.withReleaseBuildType

public class AckpineLibraryPlugin : Plugin<Project> {

	override fun apply(target: Project): Unit = target.run {
		group = rootProject.group
		version = rootProject.version
		pluginManager.run {
			apply(LibraryPlugin::class)
			apply(KotlinAndroidPluginWrapper::class)
			apply(BinaryCompatibilityValidatorPlugin::class)
		}
		configureKotlin()
		val libraryExtension = extensions.getByType<LibraryExtension>()
		extensions.create<AckpineExtension>("ackpine", libraryExtension)
		configureAndroid()
		addAssembleReleaseTasksToBuildAckpineTask()
	}

	private fun Project.configureKotlin() {
		extensions.configure<KotlinAndroidProjectExtension> {
			jvmToolchain(Constants.JDK_VERSION)
			explicitApi()

			compilerOptions {
				jvmTarget = JVM_1_8
				freeCompilerArgs.addAll("-Xjvm-default=all", "-Xconsistent-data-class-copy-visibility")
			}
		}
	}

	private fun Project.configureAndroid() = extensions.configure<LibraryExtension> {
		compileSdk = Constants.COMPILE_SDK
		buildToolsVersion = Constants.BUILD_TOOLS_VERSION

		defaultConfig {
			minSdk = Constants.MIN_SDK
			consumerProguardFiles("consumer-rules.pro")
		}

		buildTypes {
			named("release") {
				isMinifyEnabled = false
				proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
			}
		}

		compileOptions {
			sourceCompatibility = JavaVersion.VERSION_1_8
			targetCompatibility = JavaVersion.VERSION_1_8
		}
	}

	private fun Project.addAssembleReleaseTasksToBuildAckpineTask() {
		val library = configurations.create("library") {
			consumable()
			attributes {
				attribute(LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LIBRARY_ELEMENTS))
			}
		}
		extensions.configure<LibraryAndroidComponentsExtension> {
			onVariants(withReleaseBuildType()) { variant ->
				val aar = variant.artifacts.get(SingleArtifact.AAR)
				library.outgoing.artifact(aar)
			}
		}
	}

	internal companion object {
		internal const val LIBRARY_ELEMENTS = "aar"
	}
}