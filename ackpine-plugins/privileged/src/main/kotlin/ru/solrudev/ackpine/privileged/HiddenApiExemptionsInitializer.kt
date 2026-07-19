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

package ru.solrudev.ackpine.privileged

import android.content.Context
import android.os.Build
import androidx.annotation.RestrictTo
import androidx.startup.Initializer
import org.lsposed.hiddenapibypass.HiddenApiBypass

/**
 * Initializes hidden API exemptions using `androidx.startup`.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class HiddenApiExemptionsInitializer : Initializer<Unit> {

	override fun create(context: Context) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
			return
		}
		HiddenApiBypass.setHiddenApiExemptions(
			"Landroid/content/pm/IPackageManager",
			"Landroid/content/pm/IPackageInstaller",
			"Landroid/content/pm/IPackageInstallerSession",
			"Landroid/content/pm/PackageInstaller",
			"Landroid/os/UserHandle",
			"Landroid/os/ServiceManager"
		)
	}

	override fun dependencies(): List<Class<out Initializer<*>>> {
		return emptyList()
	}
}