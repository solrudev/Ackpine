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

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters

private val FILE_SEPARATOR_REGEX = Regex("[/\\\\]")

internal abstract class AckpineProjectsValueSource : ValueSource<List<String>, AckpineProjectsValueSource.Parameters> {

	override fun obtain(): List<String> {
		val rootDir = parameters.rootDirectory.get().asFile
		val rootDirPath = rootDir.toPath()
		return rootDir
			.walkTopDown()
			.onEnter { file ->
				file == rootDir
						|| (file.isDirectory
						&& file.resolve("build.gradle.kts").exists()
						&& !file.resolve("settings.gradle.kts").exists())
			}
			.filter { file -> file.isDirectory && file != rootDir }
			.map { dir ->
				rootDirPath
					.relativize(dir.toPath())
					.toString()
					.replace(FILE_SEPARATOR_REGEX, ":")
			}
			.toList()
	}

	internal interface Parameters : ValueSourceParameters {
		val rootDirectory: DirectoryProperty
	}
}