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

public data class Version(
	public val majorVersion: Int,
	public val minorVersion: Int,
	public val patchVersion: Int,
	public val suffix: String,
	public val isSnapshot: Boolean
) {
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
}