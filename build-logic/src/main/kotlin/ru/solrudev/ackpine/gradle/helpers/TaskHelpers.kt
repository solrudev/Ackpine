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

package ru.solrudev.ackpine.gradle.helpers

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskCollection

private val assembleReleaseRegex = "assemble.*Release".toRegex()

/**
 * Returns all `assemble*Release` tasks in this project.
 */
internal fun Project.assembleReleaseTasks(): TaskCollection<Task> = tasks.named { it.matches(assembleReleaseRegex) }

/**
 * Adds a [dependency] to the root task with the name of [rootTask].
 */
internal fun Project.rootTaskDependsOn(rootTask: String, dependency: Any) {
	rootProject.tasks.named(rootTask).configure {
		dependsOn(dependency)
	}
}