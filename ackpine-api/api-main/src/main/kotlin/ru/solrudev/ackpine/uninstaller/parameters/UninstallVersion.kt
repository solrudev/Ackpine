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

@file:Suppress("Unused")

package ru.solrudev.ackpine.uninstaller.parameters

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Version targeted by an uninstall session.
 *
 * Use [HIGHEST] for name-only uninstall behavior. Exact targeting works only on API level 26 or higher;
 * codes exceeding [Int.MAX_VALUE] require API level 28 or higher.
 *
 * @property code the version code of an app to uninstall.
 * @property policy policy for preserving an exact version target. Defaults to [VersionedUninstallPolicy.STRICT].
 * Ignored for [HIGHEST].
 */
public class UninstallVersion {

	public val code: Long
	public val policy: VersionedUninstallPolicy

	/**
	 * Requests an exact version [code], subject to [policy].
	 * @throws IllegalArgumentException if [code] is negative. Use [HIGHEST] for name-only behavior.
	 */
	@RequiresApi(Build.VERSION_CODES.P)
	@JvmOverloads
	public constructor(
		code: Long,
		policy: VersionedUninstallPolicy = VersionedUninstallPolicy.STRICT
	) {
		require(code >= 0) { "Version code must be non-negative" }
		this.code = code
		this.policy = policy
	}

	/**
	 * Requests an exact [Int] version [code], subject to [policy].
	 * @throws IllegalArgumentException if [code] is negative. Use [HIGHEST] for name-only behavior.
	 */
	@SuppressLint("NewApi")
	@JvmOverloads
	public constructor(
		code: Int,
		policy: VersionedUninstallPolicy = VersionedUninstallPolicy.STRICT
	) : this(code.toLong(), policy)

	private constructor() {
		code = -1
		policy = VersionedUninstallPolicy.STRICT
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false
		other as UninstallVersion
		if (code != other.code) return false
		if (policy != other.policy) return false
		return true
	}

	override fun hashCode(): Int {
		var result = code.hashCode()
		result = 31 * result + policy.hashCode()
		return result
	}

	override fun toString(): String {
		return "UninstallVersion(" +
				"code=$code, " +
				"policy=$policy" +
				")"
	}

	public companion object {

		/**
		 * Name-only uninstall targeting the highest installed version. Its [policy] has no effect.
		 */
		@JvmField
		public val HIGHEST: UninstallVersion = UninstallVersion()
	}
}