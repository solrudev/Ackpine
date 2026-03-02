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

package ru.solrudev.ackpine.impl.helpers

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build

@JvmSynthetic
internal fun Context.isPackageInstalled(packageName: String) = try {
	packageManager.getPackageInfoCompat(packageName, PackageManager.GET_ACTIVITIES)
	true
} catch (_: PackageManager.NameNotFoundException) {
	false
}

private fun PackageManager.getPackageInfoCompat(packageName: String, flags: Int): PackageInfo {
	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
		return getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
	}
	return getPackageInfo(packageName, flags)
}