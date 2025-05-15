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

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.gradle.AppPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import ru.solrudev.ackpine.gradle.Constants
import ru.solrudev.ackpine.gradle.SampleConstants
import ru.solrudev.ackpine.gradle.versioning.ackpineVersion

public class AckpineSampleBasePlugin : Plugin<Project> {

	override fun apply(target: Project): Unit = target.run {
		pluginManager.apply(AppPlugin::class)
		val applicationExtension = extensions.getByType<ApplicationExtension>()
		extensions.create<AckpineSampleBaseExtension>("ackpine", applicationExtension)
		configureJava()
		configureAndroid()
	}

	private fun Project.configureJava() = extensions.configure<JavaPluginExtension> {
		toolchain {
			languageVersion = JavaLanguageVersion.of(Constants.JDK_VERSION)
		}
	}

	private fun Project.configureAndroid() = extensions.configure<ApplicationExtension> {
		compileSdk = Constants.COMPILE_SDK
		buildToolsVersion = Constants.BUILD_TOOLS_VERSION
		namespace = SampleConstants.PACKAGE_NAME

		defaultConfig {
			applicationId = SampleConstants.PACKAGE_NAME
			minSdk = SampleConstants.MIN_SDK
			targetSdk = SampleConstants.TARGET_SDK
			versionCode = ackpineVersion.get().versionCode
			versionName = ackpineVersion.get().toString()
		}

		buildTypes.named("release") {
			isMinifyEnabled = true
			isShrinkResources = true
			proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
		}

		compileOptions {
			sourceCompatibility = SampleConstants.JAVA_VERSION
			targetCompatibility = SampleConstants.JAVA_VERSION
		}

		buildFeatures {
			viewBinding = true
		}

		lint {
			checkReleaseBuilds = false
		}
	}
}