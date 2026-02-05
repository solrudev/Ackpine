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
import org.gradle.api.artifacts.Configuration
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.language.base.plugins.LifecycleBasePlugin
import ru.solrudev.ackpine.gradle.AbiValidationAttribute.Companion.ABI_VALIDATION_CHECK_ATTRIBUTE
import ru.solrudev.ackpine.gradle.AbiValidationAttribute.Companion.ABI_VALIDATION_UPDATE_ATTRIBUTE
import ru.solrudev.ackpine.gradle.AckpineLibraryBasePlugin.Companion.ABI_CHECK
import ru.solrudev.ackpine.gradle.AckpineLibraryBasePlugin.Companion.ABI_UPDATE
import ru.solrudev.ackpine.gradle.helpers.resolvableAppArtifacts
import ru.solrudev.ackpine.gradle.helpers.resolvableLibraryArtifacts
import ru.solrudev.ackpine.gradle.tasks.ReleaseChangelogTask
import ru.solrudev.ackpine.gradle.versioning.ackpineVersion

public class AckpinePlugin : Plugin<Project> {

	override fun apply(target: Project): Unit = target.run {
		require(isolated == isolated.rootProject) {
			"Plugin must be applied to the root project but was applied to $path"
		}
		group = Constants.PACKAGE_NAME
		version = ackpineVersion.get().toString()
		val library = configurations.dependencyScope("library")
		registerBuildAckpineTask(library)
		registerAbiValidationTasks(library)
		val buildSamplesTask = registerBuildSamplesTask()
		val releaseChangelogTask = registerReleaseChangelogTask()
		configureCleanTask(buildSamplesTask, releaseChangelogTask)
	}

	private fun Project.registerBuildAckpineTask(library: Provider<out Configuration>) {
		val libraryArtifacts = configurations.resolvableLibraryArtifacts("ackpineLibraryArtifacts") {
			extendsFrom(library.get())
		}
		tasks.register("buildAckpine") {
			group = LifecycleBasePlugin.BUILD_GROUP
			description = "Assembles all Ackpine library projects."
			dependsOn(libraryArtifacts)
		}
	}

	private fun Project.registerAbiValidationTasks(library: Provider<out Configuration>) {
		val abiValidationUpdate = configurations.resolvable("abiValidationUpdate") {
			extendsFrom(library.get())
			attributes {
				attribute(ABI_VALIDATION_UPDATE_ATTRIBUTE, objects.named(ABI_UPDATE))
			}
		}
		val abiValidationCheck = configurations.resolvable("abiValidationCheck") {
			extendsFrom(library.get())
			attributes {
				attribute(ABI_VALIDATION_CHECK_ATTRIBUTE, objects.named(ABI_CHECK))
			}
		}
		tasks.register("updateAckpineAbi") {
			group = LifecycleBasePlugin.VERIFICATION_GROUP
			description = "Updates ABI dumps for all library projects."
			dependsOn(abiValidationUpdate)
		}
		tasks.register("checkAckpineAbi") {
			group = LifecycleBasePlugin.VERIFICATION_GROUP
			description = "Checks ABI for all library projects against ABI dumps."
			dependsOn(abiValidationCheck)
		}
	}

	private fun Project.registerBuildSamplesTask(): TaskProvider<*> {
		val releaseDir = layout.projectDirectory.dir("release")
		val sample = configurations.dependencyScope("sample")
		val sampleArtifacts = configurations.resolvableAppArtifacts("ackpineSampleArtifacts") {
			extendsFrom(sample.get())
		}
		return tasks.register<Sync>("buildSamples") {
			group = LifecycleBasePlugin.BUILD_GROUP
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