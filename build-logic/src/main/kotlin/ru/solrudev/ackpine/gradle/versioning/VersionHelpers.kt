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
import ru.solrudev.ackpine.gradle.helpers.withProperties

public fun Project.getVersionFromPropertiesFile(): Version = rootProject.file("version.properties").withProperties {
	val majorVersion = (get("MAJOR_VERSION") as String).toInt()
	val minorVersion = (get("MINOR_VERSION") as String).toInt()
	val patchVersion = (get("PATCH_VERSION") as String).toInt()
	val suffix = (get("SUFFIX") as String).lowercase()
	val isSnapshot = (get("SNAPSHOT") as String).toBooleanStrict()
	return Version(majorVersion, minorVersion, patchVersion, suffix, isSnapshot)
}

public val Version.versionCode: Int
	get() {
		val majorInt = majorVersion * 100000000
		val minorInt = minorVersion * 1000000
		val patchInt = patchVersion * 10000
		val suffixChar = suffix.firstOrNull()
		val suffixNumberIndex = suffix.firstOrNull { it.isDigit() } ?: ' '
		val suffixNumber = suffix.substringAfter(suffixNumberIndex, missingDelimiterValue = "0").toInt() * 10
		val suffixInt = suffixNumber + if (suffixChar != null) {
			(suffixChar.code - 'a'.code + 1).coerceAtMost(8) * 1000
		} else 9000
		val snapshotInt = if (isSnapshot) 0 else 1
		return majorInt + minorInt + patchInt + suffixInt + snapshotInt
	}