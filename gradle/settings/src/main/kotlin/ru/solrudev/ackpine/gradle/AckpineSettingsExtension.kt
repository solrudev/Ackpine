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

import org.gradle.api.file.Directory
import org.gradle.api.file.FileTree
import org.gradle.api.initialization.Settings
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.of
import javax.inject.Inject

private val SANITIZING_REGEX = Regex("\\W+")

/**
 * Extension for Ackpine `settings` plugin.
 */
public abstract class AckpineSettingsExtension @Inject constructor(private val settings: Settings) {

	/**
	 * Configures plugin management repositories.
	 */
	public fun configurePluginRepositories(): Unit = settings.pluginManagement {
		repositories.configureRepositories()
	}

	/**
	 * Finds all subprojects and adds them to the build.
	 */
	public fun includeSubprojects() {
		val rootDirectory = settings.layout.rootDirectory
		val ackpineProjects = settings.providers.of(AckpineProjectsValueSource::class) {
			parameters.rootDirectory = rootDirectory
		}
		for (project in ackpineProjects.get()) {
			settings.include(project)
		}
	}

	/**
	 * Finds a version catalog of a given [name] in `gradle` directory and registers it to the build.
	 *
	 * This function will traverse up to 2 levels up if the version catalog is not found.
	 *
	 * Provided [name] will be sanitized before usage.
	 */
	public fun versionCatalog(name: String): Unit = settings.dependencyResolutionManagement {
		val sanitizedName = name.replace(SANITIZING_REGEX, "")
		versionCatalogs.register(sanitizedName) {
			val versionCatalog = findFile(settings.layout.rootDirectory, "gradle/$sanitizedName.versions.toml")
			from(settings.layout.rootDirectory.files(versionCatalog))
		}
	}

	private tailrec fun findFile(directory: Directory, pathPattern: String, depth: Int = 0): FileTree {
		val fileTree = directory.asFileTree.matching { include(pathPattern) }
		if (!fileTree.isEmpty || depth >= 2) {
			return fileTree
		}
		return findFile(directory.dir(".."), pathPattern, depth = depth + 1)
	}
}