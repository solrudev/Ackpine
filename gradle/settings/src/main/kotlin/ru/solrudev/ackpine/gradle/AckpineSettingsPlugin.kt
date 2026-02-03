/*
 * Copyright (C) 2025 Ilya Fomichev
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

import com.android.build.api.dsl.SettingsExtension
import com.android.build.gradle.SettingsPlugin
import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.gradle.api.initialization.resolve.RepositoriesMode
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.toolchains.foojay.FoojayToolchainsConventionPlugin

public class AckpineSettingsPlugin : Plugin<Settings> {

	override fun apply(target: Settings): Unit = target.run {
		enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
		enableFeaturePreview("STABLE_CONFIGURATION_CACHE")
		pluginManager.run {
			apply(FoojayToolchainsConventionPlugin::class)
			apply(SettingsPlugin::class)
		}
		dependencyResolutionManagement {
			repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
			repositories.configureRepositories()
		}
		extensions.create<AckpineSettingsExtension>("ackpine", this)
		extensions.configure<SettingsExtension> {
			compileSdk = 35
			buildToolsVersion = "35.0.1"
			minSdk = 16
			targetSdk = 35
		}
	}
}