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

package ru.solrudev.ackpine.impl

import android.os.Build

sealed class InstallPermissionRequest(
	val requestUnknownSources: Boolean,
	val returnAfterGranting: Boolean,
	open val permissionSwitchText: String,
	open val packageName: String
) {

	data class Normal(
		override val permissionSwitchText: String,
		override val packageName: String
	) : InstallPermissionRequest(
		requestUnknownSources = true,
		returnAfterGranting = true,
		permissionSwitchText,
		packageName
	)

	data object Ignore : InstallPermissionRequest(
		requestUnknownSources = false,
		returnAfterGranting = false,
		permissionSwitchText = "",
		packageName = ""
	)

	data class Installing(
		override val permissionSwitchText: String,
		override val packageName: String
	) : InstallPermissionRequest(
		requestUnknownSources = true,
		returnAfterGranting = Build.VERSION.SDK_INT < Build.VERSION_CODES.S,
		permissionSwitchText,
		packageName
	)

	companion object {

		const val STANDARD_SWITCH = "Allow from this source"

		fun normal(
			permissionSwitchText: String,
			packageName: String
		) = Normal(permissionSwitchText, packageName)

		fun ignore() = Ignore

		fun installing(
			permissionSwitchText: String,
			packageName: String
		) = Installing(permissionSwitchText, packageName)
	}
}