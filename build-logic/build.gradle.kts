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

plugins {
	`kotlin-dsl`
}

kotlin {
	jvmToolchain(21)
	explicitApi()
}

gradlePlugin {
	plugins {
		register("ackpine") {
			id = "ru.solrudev.ackpine.project"
			implementationClass = "ru.solrudev.ackpine.gradle.AckpinePlugin"
		}
		register("ackpine-library-base") {
			id = "ru.solrudev.ackpine.library.base"
			implementationClass = "ru.solrudev.ackpine.gradle.AckpineLibraryBasePlugin"
		}
		register("ackpine-library") {
			id = "ru.solrudev.ackpine.library"
			implementationClass = "ru.solrudev.ackpine.gradle.AckpineLibraryPlugin"
		}
		register("ackpine-library-publish") {
			id = "ru.solrudev.ackpine.library-publish"
			implementationClass = "ru.solrudev.ackpine.gradle.publishing.AckpineLibraryPublishPlugin"
		}
		register("ackpine-app-release") {
			id = "ru.solrudev.ackpine.app-release"
			implementationClass = "ru.solrudev.ackpine.gradle.app.AppReleasePlugin"
		}
		register("ackpine-sample") {
			id = "ru.solrudev.ackpine.sample.base"
			implementationClass = "ru.solrudev.ackpine.gradle.app.AckpineSampleBasePlugin"
		}
		register("ackpine-kotlin-sample") {
			id = "ru.solrudev.ackpine.sample.kotlin"
			implementationClass = "ru.solrudev.ackpine.gradle.app.AckpineKotlinSamplePlugin"
		}
		register("ackpine-dokka") {
			id = "ru.solrudev.ackpine.dokka"
			implementationClass = "ru.solrudev.ackpine.gradle.documentation.DokkaConventionPlugin"
		}
	}
}

dependencies {
	implementation(libs.plugin.agp)
	implementation(libs.plugin.kotlin.android)
	implementation(libs.plugin.gradleMavenPublish)
	implementation(libs.plugin.dokka)
	implementation(libs.plugin.binaryCompatibilityValidator)
	implementation(kotlinx.serialization.json)
}