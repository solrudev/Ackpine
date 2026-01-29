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

import com.android.build.api.variant.BuiltArtifactsLoader
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import javax.inject.Inject

@DisableCachingByDefault(because = "Not worth caching")
internal abstract class PrepareSingleApkTask @Inject constructor(
	private val fs: FileSystemOperations
) : DefaultTask() {

	@get:InputFiles
	abstract val apkDirectory: DirectoryProperty

	@get:Internal
	abstract val builtArtifactsLoader: Property<BuiltArtifactsLoader>

	@get:OutputFile
	abstract val outputApk: RegularFileProperty

	@TaskAction
	fun run() {
		val dir = apkDirectory.get()
		val artifacts = builtArtifactsLoader.get().load(apkDirectory.get())
			?: error("No BuiltArtifacts metadata found in $dir")
		val element = artifacts.elements.singleOrNull()
			?: error("Expected exactly one APK, got ${artifacts.elements.size}")
		fs.sync {
			from(element.outputFile)
			into(outputApk.get().asFile.parentFile)
		}
	}
}