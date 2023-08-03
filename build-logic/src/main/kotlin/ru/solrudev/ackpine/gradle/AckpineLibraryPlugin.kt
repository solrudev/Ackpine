/*
 * Copyright (C) 2023 Ilya Fomichev
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

import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import kotlinx.validation.BinaryCompatibilityValidatorPlugin
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinAndroidPluginWrapper
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

class AckpineLibraryPlugin : Plugin<Project> {

	override fun apply(target: Project) = target.run {
		val ackpineExtension = extensions.create<AckpineExtension>("ackpine")
		afterEvaluate {
			description = ackpineExtension.artifactDescription
		}
		group = rootProject.group
		version = rootProject.version
		pluginManager.run {
			apply(LibraryPlugin::class)
			apply(KotlinAndroidPluginWrapper::class)
			apply(BinaryCompatibilityValidatorPlugin::class)
		}
		configureKotlin()
		configureAndroid()
	}

	private fun Project.configureKotlin() {
		extensions.configure<KotlinAndroidProjectExtension> {
			jvmToolchain(17)
			sourceSets.configureEach {
				languageSettings {
					enableLanguageFeature("DataObjects")
				}
			}
		}
		tasks.withType<KotlinJvmCompile>().configureEach {
			compilerOptions {
				jvmTarget.set(JVM_1_8)
				freeCompilerArgs.addAll("-Xexplicit-api=strict", "-Xjvm-default=all")
			}
		}
	}

	private fun Project.configureAndroid() = extensions.configure<LibraryExtension> {
		compileSdk = 33
		buildToolsVersion = "33.0.2"

		defaultConfig {
			minSdk = 16
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
}