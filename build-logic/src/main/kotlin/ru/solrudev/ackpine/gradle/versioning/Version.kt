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

/**
 * Version of the project, adhering to semantic versioning.
 */
public data class Version(
	public val majorVersion: Int,
	public val minorVersion: Int,
	public val patchVersion: Int,
	public val suffix: String,
	public val isSnapshot: Boolean
) : Comparable<Version> {

	/**
	 * Version code computed from semantic version number.
	 */
	public val versionCode: Int = computeVersionCode()

	override fun compareTo(other: Version): Int {
		return versionCode.compareTo(other.versionCode)
	}

	override fun toString(): String {
		return buildString {
			append("$majorVersion.$minorVersion.$patchVersion")
			if (suffix.isNotEmpty()) {
				append("-$suffix")
			}
			if (isSnapshot) {
				append("-SNAPSHOT")
			}
		}
	}

	private fun computeVersionCode(): Int {
		val suffixBase = when {
			suffix.startsWith("dev") -> 0
			suffix.startsWith("alpha") -> 1
			suffix.startsWith("beta") -> 2
			suffix.startsWith("rc") -> 8
			suffix.isEmpty() -> 9
			else -> error("Unknown version suffix")
		}
		val suffixVersionIndex = suffix.indexOfFirst { it.isDigit() }
		if (suffix.isNotEmpty() && suffixVersionIndex == -1) {
			error("No suffix version found")
		}
		val suffixVersion = if (suffixVersionIndex != -1) suffix.substring(suffixVersionIndex).toInt() else 0
		val snapshot = if (isSnapshot) 0 else 1
		return (majorVersion * 100000000 +
				minorVersion * 1000000 +
				patchVersion * 10000 +
				suffixBase * 1000 +
				suffixVersion * 10 +
				snapshot)
	}
}