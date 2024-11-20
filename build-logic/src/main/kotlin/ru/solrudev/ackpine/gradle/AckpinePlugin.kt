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

package ru.solrudev.ackpine.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.jetbrains.dokka.gradle.DokkaExtension
import org.jetbrains.dokka.gradle.DokkaPlugin
import ru.solrudev.ackpine.gradle.helpers.resolvable
import ru.solrudev.ackpine.gradle.tasks.ReleaseChangelogTask
import ru.solrudev.ackpine.gradle.versioning.versionNumber

public class AckpinePlugin : Plugin<Project> {

	private val Project.releaseChangelogFile
		get() = layout.projectDirectory.file("changelog.txt")

	private val Project.docsDir
		get() = layout.projectDirectory.dir("docs/api")

	override fun apply(target: Project): Unit = target.run {
		require(this == rootProject) { "Plugin must be applied to the root project but was applied to $path" }
		group = Constants.PACKAGE_NAME
		version = versionNumber.toString()
		pluginManager.apply(DokkaPlugin::class)
		configureDokka()
		registerBuildAckpineTask()
		registerBuildSamplesTask()
		val releaseChangelogTask = registerReleaseChangelogTask()
		configureCleanTask(releaseChangelogTask)
	}

	private fun Project.configureDokka() = extensions.configure<DokkaExtension> {
		dokkaPublications.named("html") {
			outputDirectory = docsDir
		}
	}

	private fun Project.registerBuildAckpineTask() {
		val library = configurations.register("library") {
			resolvable()
			attributes {
				attribute(LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(AckpineLibraryPlugin.LIBRARY_ELEMENTS))
			}
		}
		tasks.register("buildAckpine") {
			group = "build"
			description = "Assembles all Ackpine library projects."
			dependsOn(library)
		}
	}

	private fun Project.registerBuildSamplesTask() {
		val sample = configurations.register("sample") {
			resolvable()
			attributes {
				attribute(LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(AppReleasePlugin.LIBRARY_ELEMENTS))
			}
		}
		tasks.register("buildSamples") {
			group = "build"
			description = "Builds and gathers all Ackpine sample app APKs."
			dependsOn(sample)
		}
	}

	private fun Project.registerReleaseChangelogTask(): TaskProvider<*> {
		return tasks.register<ReleaseChangelogTask>("releaseChangelog") {
			changelogFile = layout.projectDirectory.file("docs/changelog.md")
			outputFile = releaseChangelogFile
		}
	}

	private fun Project.configureCleanTask(deleteTarget: Any) {
		tasks.named<Delete>("clean") {
			delete(layout.buildDirectory)
			delete(docsDir)
			delete(deleteTarget)
		}
	}
}