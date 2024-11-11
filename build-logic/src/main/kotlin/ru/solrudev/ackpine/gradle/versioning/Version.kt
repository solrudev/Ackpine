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
) {

	/**
	 * Version code computed from semantic version number.
	 */
	public val versionCode: Int = computeVersionCode()

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
		val majorInt = majorVersion * 100000000
		val minorInt = minorVersion * 1000000
		val patchInt = patchVersion * 10000
		val suffixBaseInt = when {
			suffix.startsWith("dev") -> 0
			suffix.startsWith("alpha") -> 1000
			suffix.startsWith("beta") -> 2000
			suffix.startsWith("rc") -> 8000
			suffix.isEmpty() -> 9000
			else -> error("Unknown version suffix")
		}
		val suffixVersionIndex = suffix.indexOfFirst { it.isDigit() }
		val suffixVersionInt = if (suffixVersionIndex != -1) suffix.substring(suffixVersionIndex).toInt() * 10 else 0
		val suffixInt = suffixBaseInt + suffixVersionInt
		val snapshotInt = if (isSnapshot) 0 else 1
		return majorInt + minorInt + patchInt + suffixInt + snapshotInt
	}
}