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

@file:JvmName("PackageUninstallerImpl")

package ru.solrudev.ackpine.test.uninstaller

import android.content.Context
import ru.solrudev.ackpine.test.TestPackageUninstaller

@Volatile
private var packageUninstaller: TestPackageUninstaller? = null

private val lock = Any()

/**
 * Links to [ru.solrudev.ackpine.uninstaller.PackageUninstaller.getInstance] in `ackpine-api` and returns
 * [TestPackageUninstaller].
 *
 * This allows production code that relies on `getInstance` to receive an in-memory test double in unit tests.
 */
@Suppress("Unused")
@JvmName("getInstance")
@JvmSynthetic
internal fun getInstance(context: Context): TestPackageUninstaller {
	var instance = packageUninstaller
	if (instance != null) {
		return instance
	}
	synchronized(lock) {
		instance = packageUninstaller
		if (instance == null) {
			instance = TestPackageUninstaller()
			packageUninstaller = instance
		}
	}
	return instance!!
}