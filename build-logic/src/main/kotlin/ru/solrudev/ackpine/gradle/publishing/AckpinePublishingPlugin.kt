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

package ru.solrudev.ackpine.gradle.publishing

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.withType
import org.jetbrains.dokka.gradle.DokkaMultiModuleTask
import org.jetbrains.dokka.gradle.DokkaPlugin
import ru.solrudev.ackpine.gradle.Constants
import ru.solrudev.ackpine.gradle.versioning.versionNumber

public class AckpinePublishingPlugin : Plugin<Project> {

	override fun apply(target: Project): Unit = target.run {
		require(this == rootProject) { "Plugin must be applied to the root project but was applied to $path" }
		group = Constants.PACKAGE_NAME
		version = versionNumber.toString()
		pluginManager.apply(DokkaPlugin::class)
		tasks.withType<DokkaMultiModuleTask>().configureEach {
			outputDirectory = layout.projectDirectory.dir("docs/api")
		}
		registerBuildAckpineTask()
	}

	private fun Project.registerBuildAckpineTask() = tasks.register("buildAckpine") {
		group = "build"
		description = "Assembles all Ackpine library projects."
	}
}