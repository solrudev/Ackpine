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
import ru.solrudev.ackpine.gradle.helpers.getOrThrow
import ru.solrudev.ackpine.gradle.helpers.toPropertiesMap

private const val PARSED_VERSION = "parsedVersion"

/**
 * Returns a [Version] object parsed from `version.properties` file in root project directory.
 */
public val Project.versionNumber: Version
	get() {
		if (rootProject.hasProperty(PARSED_VERSION)) {
			return rootProject.extra[PARSED_VERSION] as Version
		}
		val versionProperties = rootProject.file("version.properties").toPropertiesMap()
		val majorVersion = versionProperties.getOrThrow("MAJOR_VERSION").toInt()
		val minorVersion = versionProperties.getOrThrow("MINOR_VERSION").toInt()
		val patchVersion = versionProperties.getOrThrow("PATCH_VERSION").toInt()
		check("SUFFIX" in versionProperties.keys) { "SUFFIX was not provided" }
		val suffix = (versionProperties["SUFFIX"] as String).lowercase()
		val isSnapshot = versionProperties.getOrThrow("SNAPSHOT").toBooleanStrict()
		return Version(majorVersion, minorVersion, patchVersion, suffix, isSnapshot).also { version ->
			rootProject.extra[PARSED_VERSION] = version
		}
	}