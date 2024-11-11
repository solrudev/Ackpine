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

import ru.solrudev.ackpine.gradle.Constants
import ru.solrudev.ackpine.gradle.SampleConstants
import ru.solrudev.ackpine.gradle.versioning.versionNumber

description = "Sample application in Java showcasing Ackpine usage"

plugins {
	id(libs.plugins.android.application.get().pluginId)
}

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(Constants.JDK_VERSION)
	}
}

android {
	compileSdk = Constants.COMPILE_SDK
	buildToolsVersion = Constants.BUILD_TOOLS_VERSION
	namespace = SampleConstants.PACKAGE_NAME

	defaultConfig {
		applicationId = SampleConstants.PACKAGE_NAME
		minSdk = SampleConstants.MIN_SDK
		targetSdk = SampleConstants.TARGET_SDK
		versionCode = versionNumber.versionCode
		versionName = versionNumber.toString()
	}

	buildTypes {
		named("release") {
			isMinifyEnabled = true
			isShrinkResources = true
			proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
		}
	}

	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17
	}

	buildFeatures {
		viewBinding = true
	}
}

dependencies {
	implementation(projects.ackpineCore)
	implementation(projects.ackpineSplits)
	implementation(projects.ackpineResources)
	implementation(androidx.activity)
	implementation(androidx.appcompat)
	implementation(androidx.recyclerview)
	implementation(androidx.constraintlayout)
	implementation(androidx.coordinatorlayout)
	implementation(androidx.bundles.lifecycle)
	implementation(androidx.bundles.navigation)
	implementation(androidx.swiperefreshlayout)
	implementation(libs.materialcomponents)
	implementation(libs.guava)
}