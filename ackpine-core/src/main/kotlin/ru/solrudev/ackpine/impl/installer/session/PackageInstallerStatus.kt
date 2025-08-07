/*
 * Copyright (C) 2025 Ilya Fomichev
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

package ru.solrudev.ackpine.impl.installer.session

import androidx.annotation.RestrictTo

// https://cs.android.com/android/platform/superproject/main/+/main:frameworks/base/core/java/android/content/pm/PackageManager.java;drc=82ada6503a81af7eeed2924a2d2d942375f6c8c2;l=2099
/**
 * The status as used internally in the package manager.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal enum class PackageInstallerStatus(
	@get:JvmSynthetic internal val legacyStatus: Int,
	@get:JvmSynthetic internal val isPreapproval: Boolean = false
) {

	/**
	 * Installation failed return code: requesting user pre-approval is currently unavailable.
	 */
	INSTALL_FAILED_PRE_APPROVAL_NOT_AVAILABLE(-129, isPreapproval = true);

	internal companion object {

		/**
		 * Returns an enum entry matching the int status.
		 */
		@JvmSynthetic
		internal fun fromLegacyStatus(legacyStatus: Int) = entries.firstOrNull { it.legacyStatus == legacyStatus }
	}
}