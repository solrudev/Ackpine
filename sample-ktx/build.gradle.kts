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

import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import ru.solrudev.ackpine.gradle.Constants
import ru.solrudev.ackpine.gradle.SampleConstants
import ru.solrudev.ackpine.gradle.versioning.versionNumber

description = "Sample application in Kotlin showcasing Ackpine usage and leveraging ackpine-ktx extensions"

plugins {
	id(libs.plugins.android.application.get().pluginId)
	id(libs.plugins.kotlin.android.get().pluginId)
	id("ru.solrudev.ackpine.app-release")
}

kotlin {
	jvmToolchain(Constants.JDK_VERSION)
}

android {
	compileSdk = Constants.COMPILE_SDK
	buildToolsVersion = Constants.BUILD_TOOLS_VERSION
	namespace = SampleConstants.PACKAGE_NAME

	defaultConfig {
		applicationId = SampleConstants.PACKAGE_NAME
		minSdk = SampleConstants.MIN_SDK
		targetSdk = SampleConstants.TARGET_SDK
		versionCode = versionNumber.get().versionCode
		versionName = versionNumber.get().toString()
	}

	buildTypes {
		named("release") {
			isMinifyEnabled = true
			isShrinkResources = true
			proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
		}
	}

	compileOptions {
		sourceCompatibility = SampleConstants.JAVA_VERSION
		targetCompatibility = SampleConstants.JAVA_VERSION
	}

	buildFeatures {
		viewBinding = true
	}
}

tasks.withType<KotlinJvmCompile>().configureEach {
	compilerOptions {
		freeCompilerArgs.add("-Xjvm-default=all")
	}
}

dependencies {
	implementation(projects.ackpineSplits)
	implementation(projects.ackpineKtx)
	implementation(projects.ackpineResources)
	implementation(androidx.activity)
	implementation(androidx.appcompat)
	implementation(androidx.recyclerview)
	implementation(androidx.constraintlayout)
	implementation(androidx.coordinatorlayout)
	implementation(androidx.lifecycle.viewmodel)
	implementation(androidx.bundles.navigation)
	implementation(androidx.swiperefreshlayout)
	implementation(libs.materialcomponents)
	implementation(libs.viewbindingpropertydelegate)
}