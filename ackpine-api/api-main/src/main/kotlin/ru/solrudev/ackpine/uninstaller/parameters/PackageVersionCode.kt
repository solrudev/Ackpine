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

package ru.solrudev.ackpine.uninstaller.parameters

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import ru.solrudev.ackpine.uninstaller.parameters.PackageVersionCode.Companion.HIGHEST
import java.io.Serializable

/**
 * Version code targeted by an uninstall session.
 *
 * Use [HIGHEST] for name-only uninstall behavior. Exact targeting works only on API level 26 or higher;
 * codes exceeding [Int.MAX_VALUE] require API level 28 or higher.
 *
 * @param value the version code, or `-1` for [HIGHEST].
 * @throws IllegalArgumentException if [value] is less than `-1`.
 */
public class PackageVersionCode @RequiresApi(Build.VERSION_CODES.P) public constructor(
	/** The version code, or `-1` for [HIGHEST]. */
	public val value: Long
) : Serializable {

	/**
	 * Creates a version code for an [Int] [value].
	 * @param value the version code, or `-1` for [HIGHEST].
	 * @throws IllegalArgumentException if [value] is less than `-1`.
	 */
	@SuppressLint("NewApi")
	public constructor(value: Int) : this(value.toLong())

	init {
		require(value >= -1) { "Version code must be -1 or non-negative" }
	}

	override fun equals(other: Any?): Boolean = other is PackageVersionCode && value == other.value
	override fun hashCode(): Int = value.hashCode()
	override fun toString(): String = "PackageVersionCode($value)"

	public companion object {

		@Suppress("Unused")
		private const val serialVersionUID: Long = 1503614119542437082L

		/**
		 * Name-only uninstall targeting the highest installed version.
		 */
		@JvmField
		public val HIGHEST: PackageVersionCode = PackageVersionCode(-1)
	}
}