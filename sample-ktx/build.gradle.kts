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

import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import ru.solrudev.ackpine.gradle.Constants

description = "Sample application in Kotlin showcasing Ackpine usage and leveraging ackpine-ktx extensions"

plugins {
	id(libs.plugins.android.application.get().pluginId)
	id(libs.plugins.kotlin.android.get().pluginId)
}

kotlin {
	jvmToolchain(17)
}

android {
	compileSdk = 33
	buildToolsVersion = "33.0.2"
	namespace = Constants.samplePackageName

	defaultConfig {
		applicationId = Constants.samplePackageName
		minSdk = 21
		targetSdk = 33
		versionCode = 1
		versionName = rootProject.version.toString()
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

tasks.withType<KotlinJvmCompile>().configureEach {
	compilerOptions {
		freeCompilerArgs.add("-Xjvm-default=all")
	}
}

dependencies {
	implementation(projects.ackpineSplits)
	implementation(projects.ackpineKtx)
	implementation(androidx.activity.ktx)
	implementation(androidx.appcompat)
	implementation(androidx.recyclerview)
	implementation(androidx.constraintlayout)
	implementation(androidx.bundles.lifecycle.ktx)
	implementation(androidx.bundles.navigation.ktx)
	implementation(androidx.swiperefreshlayout)
	implementation(libs.materialcomponents)
	implementation(libs.viewbindingpropertydelegate)
}