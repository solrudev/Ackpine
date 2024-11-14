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

rootProject.name = "Ackpine"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
enableFeaturePreview("STABLE_CONFIGURATION_CACHE")

pluginManagement {
	includeBuild("build-logic")

	repositories {
		gradlePluginPortal()
		google()
		mavenCentral()
	}
}

plugins {
	id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

dependencyResolutionManagement {
	repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS

	repositories {
		gradlePluginPortal()
		google()
		mavenCentral()
	}

	versionCatalogs {
		register("androidx") {
			from(files("gradle/androidx.versions.toml"))
		}
		register("kotlinx") {
			from(files("gradle/kotlinx.versions.toml"))
		}
	}
}

include(":ackpine-core")
include(":ackpine-ktx")
include(":ackpine-splits")
include(":ackpine-assets")
include(":ackpine-runtime")
include(":ackpine-resources")
include(":sample-java")
include(":sample-ktx")