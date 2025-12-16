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

package ru.solrudev.ackpine.impl.uninstaller.activity

import android.os.Bundle
import androidx.annotation.RestrictTo
import ru.solrudev.ackpine.impl.activity.SessionCommitActivity
import ru.solrudev.ackpine.impl.uninstaller.PackageUninstallerImpl
import ru.solrudev.ackpine.uninstaller.UninstallFailure

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal abstract class UninstallActivity protected constructor(tag: String) : SessionCommitActivity<UninstallFailure>(
	tag, abortedStateFailureFactory = UninstallFailure::Aborted
) {

	override val ackpineSessionFuture by lazy(LazyThreadSafetyMode.NONE) {
		ackpinePackageUninstaller.getSessionAsync(ackpineSessionId)
	}

	private lateinit var ackpinePackageUninstaller: PackageUninstallerImpl

	override fun onCreate(savedInstanceState: Bundle?) {
		ackpinePackageUninstaller = PackageUninstallerImpl.getInstance(this)
		super.onCreate(savedInstanceState)
	}
}