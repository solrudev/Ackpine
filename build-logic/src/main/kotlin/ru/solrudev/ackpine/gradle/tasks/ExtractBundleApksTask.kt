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

import com.android.build.api.variant.Aapt2
import org.gradle.api.DefaultTask
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.assign
import org.gradle.process.ExecOperations
import org.gradle.api.tasks.CacheableTask
import javax.inject.Inject

@CacheableTask
internal abstract class ExtractBundleApksTask @Inject constructor(
	private val exec: ExecOperations,
	private val fs: FileSystemOperations,
	private val archives: ArchiveOperations
) : DefaultTask() {

	@get:InputFile
	@get:PathSensitive(PathSensitivity.NONE)
	abstract val bundleFile: RegularFileProperty

	@get:InputFiles
	@get:Classpath
	abstract val bundletoolClasspath: ConfigurableFileCollection

	@get:Nested
	abstract val aapt2: Property<Aapt2>

	@get:InputFile
	@get:PathSensitive(PathSensitivity.NONE)
	abstract val keystoreFile: RegularFileProperty

	@get:Input
	abstract val keyAlias: Property<String>

	@get:Input
	abstract val keyPassword: Property<String>

	@get:Input
	abstract val storePassword: Property<String>

	@get:Input
	abstract val artifactDirectoryName: Property<String>

	@get:OutputDirectory
	abstract val outputDirectory: DirectoryProperty

	@TaskAction
	fun extract() {
		val apksFile = temporaryDir.resolve("output.apks")
		exec.javaexec {
			classpath(bundletoolClasspath)
			mainClass = "com.android.tools.build.bundletool.BundleToolMain"
			args(
				"build-apks",
				"--bundle", bundleFile.get().asFile.absolutePath,
				"--output", apksFile.absolutePath,
				"--overwrite",
				"--aapt2", aapt2.get().executable.get().asFile.absolutePath,
				"--ks", keystoreFile.get().asFile.absolutePath,
				"--ks-key-alias", keyAlias.get(),
				"--ks-pass", "pass:${storePassword.get()}",
				"--key-pass", "pass:${keyPassword.get()}"
			)
		}
		fs.sync {
			from(archives.zipTree(apksFile)) {
				include("**/*.apk")
				eachFile { path = name }
			}
			from(apksFile) {
				rename { "bundle.apks" }
			}
			into(outputDirectory.dir(artifactDirectoryName))
			includeEmptyDirs = false
		}
	}
}