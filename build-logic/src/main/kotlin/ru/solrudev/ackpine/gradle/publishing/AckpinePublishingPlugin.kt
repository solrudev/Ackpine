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
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.jetbrains.dokka.gradle.DokkaExtension
import org.jetbrains.dokka.gradle.DokkaPlugin
import ru.solrudev.ackpine.gradle.Constants
import ru.solrudev.ackpine.gradle.tasks.BuildAckpineTask
import ru.solrudev.ackpine.gradle.tasks.BuildReleaseSamplesTask
import ru.solrudev.ackpine.gradle.tasks.ReleaseChangelogTask
import ru.solrudev.ackpine.gradle.versioning.versionNumber

public class AckpinePublishingPlugin : Plugin<Project> {

	private val Project.releaseChangelog
		get() = layout.projectDirectory.file("changelog.txt")

	override fun apply(target: Project): Unit = target.run {
		require(this == rootProject) { "Plugin must be applied to the root project but was applied to $path" }
		group = Constants.PACKAGE_NAME
		version = versionNumber.toString()
		pluginManager.apply(DokkaPlugin::class)
		configureDokka()
		registerBuildAckpineTask()
		val buildReleaseSamplesTask = registerBuildSamplesReleaseTask()
		val releaseChangelogTask = registerReleaseChangelogTask()
		configureCleanTask(buildReleaseSamplesTask, releaseChangelogTask)
	}

	private fun Project.configureDokka() = extensions.configure<DokkaExtension> {
		dokkaPublications.named("html") {
			outputDirectory = layout.projectDirectory.dir("docs/api")
		}
	}

	private fun Project.registerBuildAckpineTask() {
		tasks.register<BuildAckpineTask>("buildAckpine")
	}

	private fun Project.registerBuildSamplesReleaseTask(): TaskProvider<*> {
		return tasks.register<BuildReleaseSamplesTask>("buildReleaseSamples")
	}

	private fun Project.registerReleaseChangelogTask(): TaskProvider<*> {
		return tasks.register<ReleaseChangelogTask>("releaseChangelog") {
			changelogFile = layout.projectDirectory.file("docs/changelog.md")
			releaseChangelogFile = releaseChangelog
		}
	}

	private fun Project.configureCleanTask(vararg producingTasks: TaskProvider<*>) {
		tasks.named<Delete>("clean") {
			delete(project.rootProject.layout.buildDirectory)
			delete(*producingTasks)
		}
	}
}