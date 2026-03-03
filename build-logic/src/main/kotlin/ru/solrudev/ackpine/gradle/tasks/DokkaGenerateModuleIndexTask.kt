/*
 * Copyright (C) 2026 Ilya Fomichev
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
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Not worth caching")
internal abstract class DokkaGenerateModuleIndexTask : DefaultTask() {

	@get:Input
	internal abstract val moduleDisplayName: Property<String>

	@get:Input
	internal abstract val moduleDescription: Property<String>

	@get:OutputFile
	internal abstract val outputFile: RegularFileProperty

	@TaskAction
	internal fun writeIndexFile() {
		val file = outputFile.asFile.get()
		val moduleName = moduleDisplayName.get()
		val moduleDescription = moduleDescription.get()
		file.parentFile?.mkdirs()
		file.createNewFile()
		file.writeText(
			"""
			# Module $moduleName
			
			$moduleDescription
			""".trimIndent()
		)
	}
}