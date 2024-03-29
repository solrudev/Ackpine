/*
 * Copyright (C) 2023 Ilya Fomichev
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

package ru.solrudev.ackpine.impl.uninstaller.helpers

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build

@JvmSynthetic
internal fun PackageManager.getApplicationLabel(packageName: String) = try {
	getApplicationInfoCompat(packageName, PackageManager.GET_META_DATA).loadLabel(this)
} catch (_: PackageManager.NameNotFoundException) {
	null
}

@Suppress("DEPRECATION")
private fun PackageManager.getApplicationInfoCompat(packageName: String, flags: Int): ApplicationInfo {
	return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
		getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(flags.toLong()))
	} else {
		getApplicationInfo(packageName, flags)
	}
}