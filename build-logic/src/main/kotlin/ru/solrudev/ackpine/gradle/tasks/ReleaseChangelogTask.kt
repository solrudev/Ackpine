/*
 * Copyright (C) 2024 Ilya Fomichev
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

package ru.solrudev.ackpine.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

internal abstract class ReleaseChangelogTask : DefaultTask() {

	@get:InputFile
	internal abstract val changelogFile: RegularFileProperty

	@get:OutputFile
	internal abstract val outputFile: RegularFileProperty

	override fun getGroup(): String = "documentation"
	override fun getDescription(): String = "Extracts changelog for the last release."

	@TaskAction
	internal fun writeReleaseChangelog() {
		val changelog = changelogFile.get().asFile.useLines { lines ->
			lines
				.dropWhile { !it.startsWith("### ") }
				.takeWhile { !it.startsWith("Version") }
				.toList()
				.dropLastWhile { it.isEmpty() }
				.joinToString("\n")
		}
		outputFile.get().asFile.writeText(changelog)
	}
}