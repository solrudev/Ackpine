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

package ru.solrudev.ackpine.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import ru.solrudev.ackpine.gradle.app.AppReleasePlugin
import ru.solrudev.ackpine.gradle.helpers.libraryElements
import ru.solrudev.ackpine.gradle.tasks.ReleaseChangelogTask
import ru.solrudev.ackpine.gradle.versioning.ackpineVersion

public class AckpinePlugin : Plugin<Project> {

	override fun apply(target: Project): Unit = target.run {
		require(isolated == isolated.rootProject) {
			"Plugin must be applied to the root project but was applied to $path"
		}
		group = Constants.PACKAGE_NAME
		version = ackpineVersion.get().toString()
		registerBuildAckpineTask()
		val buildSamplesTask = registerBuildSamplesTask()
		val releaseChangelogTask = registerReleaseChangelogTask()
		configureCleanTask(buildSamplesTask, releaseChangelogTask)
	}

	private fun Project.registerBuildAckpineTask() {
		val library = configurations.dependencyScope("library")
		val libraryArtifacts = configurations.resolvable("ackpineLibraryArtifacts") {
			extendsFrom(library.get())
			libraryElements(objects.named(AckpineLibraryBasePlugin.LIBRARY_ELEMENTS))
		}
		tasks.register("buildAckpine") {
			group = "build"
			description = "Assembles all Ackpine library projects."
			dependsOn(libraryArtifacts)
		}
	}

	private fun Project.registerBuildSamplesTask(): TaskProvider<*> {
		val releaseDir = layout.projectDirectory.dir("release")
		val sample = configurations.dependencyScope("sample")
		val sampleArtifacts = configurations.resolvable("ackpineSampleArtifacts") {
			extendsFrom(sample.get())
			libraryElements(objects.named(AppReleasePlugin.LIBRARY_ELEMENTS))
		}
		return tasks.register<Sync>("buildSamples") {
			group = "build"
			description = "Builds and gathers all Ackpine sample app APKs."
			from(sampleArtifacts)
			into(releaseDir)
		}
	}

	private fun Project.registerReleaseChangelogTask(): TaskProvider<*> {
		val releaseChangelogFile = layout.projectDirectory.file("changelog.txt")
		return tasks.register<ReleaseChangelogTask>("releaseChangelog") {
			changelogFile.convention(layout.docsDirectory.file("changelog.md"))
			outputFile.convention(releaseChangelogFile)
		}
	}

	private fun Project.configureCleanTask(vararg deleteTargets: Any) {
		tasks.register<Delete>("clean") {
			delete(layout.buildDirectory)
			delete(*deleteTargets)
		}
	}
}