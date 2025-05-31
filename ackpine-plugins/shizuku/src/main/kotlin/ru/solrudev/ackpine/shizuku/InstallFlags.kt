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

package ru.solrudev.ackpine.shizuku

import android.annotation.SuppressLint
import android.content.pm.PackageInstaller

@JvmSynthetic
internal const val INSTALL_BYPASS_LOW_TARGET_SDK_BLOCK = 0x01000000

@JvmSynthetic
internal const val INSTALL_ALLOW_TEST = 0x00000004

@JvmSynthetic
internal const val INSTALL_REPLACE_EXISTING = 0x00000002

@JvmSynthetic
internal const val INSTALL_REQUEST_DOWNGRADE = 0x00000080

@JvmSynthetic
internal const val INSTALL_ALLOW_DOWNGRADE = 0x00100000

@JvmSynthetic
internal const val INSTALL_GRANT_ALL_REQUESTED_PERMISSIONS = 0x00000100

@JvmSynthetic
internal const val INSTALL_ALL_USERS = 0x00000040

private val INSTALL_FLAGS by lazy {
	@SuppressLint("PrivateApi")
	PackageInstaller.SessionParams::class.java.getDeclaredField("installFlags")
}

@JvmSynthetic
internal fun PackageInstaller.SessionParams.getInstallFlags(): Int {
	return INSTALL_FLAGS.get(this) as Int
}

@JvmSynthetic
internal fun PackageInstaller.SessionParams.setInstallFlags(flags: Int) {
	INSTALL_FLAGS.set(this, flags)
}