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

plugins {
	`kotlin-dsl`
}

kotlin {
	jvmToolchain(17)
	explicitApi()
}

gradlePlugin {
	plugins {
		register("ackpine") {
			id = "ru.solrudev.ackpine.library"
			implementationClass = "ru.solrudev.ackpine.gradle.AckpineLibraryPlugin"
		}
		register("ackpine-library-publish") {
			id = "ru.solrudev.ackpine.library-publish"
			implementationClass = "ru.solrudev.ackpine.gradle.publishing.AckpineLibraryPublishPlugin"
		}
		register("ackpine-publishing") {
			id = "ru.solrudev.ackpine.publishing"
			implementationClass = "ru.solrudev.ackpine.gradle.publishing.AckpinePublishingPlugin"
		}
		register("ackpine-app-release-signing") {
			id = "ru.solrudev.ackpine.app-release-signing"
			implementationClass = "ru.solrudev.ackpine.gradle.AppReleaseSigningPlugin"
		}
	}
}

dependencies {
	implementation(libs.plugin.agp)
	implementation(libs.plugin.kotlin.android)
	implementation(libs.plugin.gradleMavenPublish)
	implementation(libs.plugin.dokka)
	implementation(libs.plugin.binaryCompatibilityValidator)
}