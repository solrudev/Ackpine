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

package ru.solrudev.ackpine.gradle.versioning

import org.gradle.api.Project
import org.gradle.kotlin.dsl.extra
import ru.solrudev.ackpine.gradle.helpers.withProperties

private const val PARSED_VERSION = "parsedVersion"

/**
 * Returns a [Version] object parsed from `version.properties` file in root project directory.
 */
public fun Project.getVersionFromPropertiesFile(): Version {
	if (rootProject.hasProperty(PARSED_VERSION)) {
		return rootProject.extra[PARSED_VERSION] as Version
	}
	rootProject.file("version.properties").withProperties { properties ->
		val majorVersion = (properties["MAJOR_VERSION"] as String).toInt()
		val minorVersion = (properties["MINOR_VERSION"] as String).toInt()
		val patchVersion = (properties["PATCH_VERSION"] as String).toInt()
		val suffix = (properties["SUFFIX"] as String).lowercase()
		val isSnapshot = (properties["SNAPSHOT"] as String).toBooleanStrict()
		return Version(majorVersion, minorVersion, patchVersion, suffix, isSnapshot).also { version ->
			rootProject.extra[PARSED_VERSION] = version
		}
	}
}