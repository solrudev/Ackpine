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

import ru.solrudev.ackpine.gradle.helpers.builtApk

description = "A library providing consistent APIs for installing and uninstalling apps on an Android device"

plugins {
	id("ru.solrudev.ackpine.library")
	id("ru.solrudev.ackpine.library-publish")
	id("ru.solrudev.ackpine.dokka")
	id("ru.solrudev.ackpine.asset-app-artifacts")
	id("ru.solrudev.ackpine.jacoco")
	alias(libs.plugins.kotlin.ksp)
	alias(androidx.plugins.room)
}

ackpine {
	id = "core"
	testing {
		enableHostTests = true
		enableDeviceTests = true
	}
	artifact {
		name = "Ackpine Core"
	}
	internalPackages("ru.solrudev.ackpine.impl")
}

room {
	schemaDirectory(layout.projectDirectory.dir("schemas"))
}

assetAppArtifacts {
	components(withDebugBuildType()) {
		consumeIn(androidTest)
	}
}

android {
	defaultConfig {
		testInstrumentationRunnerArguments["filter"] = "ru.solrudev.ackpine.impl.testutil.DeviceAwareTestFilter"
	}
	sourceSets {
		named("test") {
			assets.directories.add(layout.projectDirectory.dir("schemas").toString())
		}
	}
	testOptions {
		unitTests.isIncludeAndroidResources = true
		managedDevices {
			localDevices.register("api27") {
				device = "Pixel 2"
				sdkVersion = 27
				systemImageSource = "aosp"
				require64Bit = true
			}
			localDevices.register("api30") {
				device = "Pixel 5"
				sdkVersion = 30
				systemImageSource = "aosp"
				require64Bit = true
			}
			localDevices.register("api31") {
				device = "Pixel 6"
				sdkVersion = 31
				systemImageSource = "aosp"
				require64Bit = true
			}
			localDevices.register("api34") {
				device = "Pixel 8"
				sdkVersion = 34
				systemImageSource = "aosp"
				require64Bit = true
			}
		}
	}
}

dependencies {
	ksp(androidx.room.compiler)
	api(androidx.startup)
	api(projects.ackpineApi.apiMain)
	implementation(projects.ackpineRuntime)
	implementation(androidx.concurrent.futures.core)
	implementation(androidx.core.ktx)
	implementation(androidx.room.runtime)
	testImplementation(libs.kotlin.test)
	testImplementation(libs.robolectric)
	testImplementation(kotlinx.coroutines.test)
	testImplementation(androidx.test.core)
	testImplementation(androidx.room.testing)
	testImplementation(projects.ackpineKtx)
	androidTestImplementation(libs.kotlin.test)
	androidTestImplementation(androidx.bundles.test)
	androidTestImplementation(kotlinx.coroutines.android)
	androidTestImplementation(kotlinx.coroutines.test)
	androidTestImplementation(projects.ackpineKtx)
	androidTestImplementation(projects.testFixtures.remoteApi)
	androidTestUtil(builtApk(projects.testFixtures.installerApp))
	assetAppArtifacts(projects.testFixtures.apkFixture)
	assetAppArtifacts(projects.testFixtures.installerApp)
}