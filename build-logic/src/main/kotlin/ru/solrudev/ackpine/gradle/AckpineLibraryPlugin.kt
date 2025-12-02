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

import com.android.build.api.dsl.LibraryExtension
import kotlinx.validation.BinaryCompatibilityValidatorPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.findByType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

public class AckpineLibraryPlugin : Plugin<Project> {

	override fun apply(target: Project): Unit = target.run {
		pluginManager.run {
			apply(BinaryCompatibilityValidatorPlugin::class)
			apply(AckpineLibraryBasePlugin::class)
		}
		configureAndroid()
		configureKotlin()
	}

	private fun Project.configureAndroid() = extensions.configure<LibraryExtension> {
		enableKotlin = true
	}

	@Suppress("NewApi")
	private fun Project.configureKotlin() = extensions.configure<KotlinAndroidExtension> {
		val stdlibVersion = extensions
			.findByType<VersionCatalogsExtension>()
			?.named("libs")
			?.findVersion("kotlin-for-consumers")
			?.get()
			?.displayName
			?: coreLibrariesVersion
		val kotlinVersion = KotlinVersion.fromVersion(
			stdlibVersion
				.split('.')
				.take(2)
				.joinToString(".")
		)

		coreLibrariesVersion = stdlibVersion
		explicitApi()

		compilerOptions {
			languageVersion = kotlinVersion
			apiVersion = kotlinVersion
			jvmTarget = JVM_1_8
			freeCompilerArgs.addAll("-Xjvm-default=all", "-Xconsistent-data-class-copy-visibility")
		}
	}
}