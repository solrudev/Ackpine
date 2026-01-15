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

import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.AbstractCopyTask
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.Sync
import org.gradle.kotlin.dsl.assign
import java.io.File

internal abstract class PrepareAssetAppArtifactsTask : Sync() {

	@get:OutputDirectory
	abstract val outputDirectory: DirectoryProperty

	override fun into(destDir: Any): AbstractCopyTask {
		@Suppress("UNCHECKED_CAST")
		when (destDir) {
			is File -> outputDirectory = destDir
			is Directory -> outputDirectory = destDir
			else -> outputDirectory = destDir as? Provider<Directory> ?: return super.into(destDir)
		}
		return super.into(destDir)
	}
}