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

package ru.solrudev.ackpine

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.LOLLIPOP)
@JvmSynthetic
internal fun isPackageInstallerApiAvailable(): Boolean = isPackageInstallerAvailable

// To avoid referencing Build.VERSION.SDK_INT
private val isPackageInstallerAvailable = try {
	Class.forName("android.content.pm.PackageInstaller")
	true
} catch (_: ClassNotFoundException) {
	false
}