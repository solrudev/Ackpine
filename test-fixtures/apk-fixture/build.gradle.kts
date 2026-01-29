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

description = "Builds APK files to use in tests"

plugins {
	id("ru.solrudev.ackpine.sample.base")
	id("ru.solrudev.ackpine.app-release")
}

ackpine {
	id = "fixture"
	minSdk = 16
}

android {
	defaultConfig {
		versionCode = 1
		versionName = "1.0"
	}
	buildTypes {
		named("release") {
			isMinifyEnabled = false
			isShrinkResources = false
		}
	}
	val flavorDimension = "apk"
	flavorDimensions += flavorDimension
	productFlavors {
		register("v1") {
			dimension = flavorDimension
		}
		register("minSdk") {
			dimension = flavorDimension
			minSdk = 33
		}
	}
}