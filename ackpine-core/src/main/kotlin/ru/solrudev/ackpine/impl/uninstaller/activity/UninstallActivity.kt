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
import ru.solrudev.ackpine.uninstaller.PackageUninstaller
import ru.solrudev.ackpine.uninstaller.UninstallFailure

private const val TAG = "UninstallActivity"

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class UninstallActivity : SessionCommitActivity<UninstallFailure>(
	TAG, abortedStateFailureFactory = UninstallFailure::Aborted
) {

	override val ackpineSessionFuture by lazy(LazyThreadSafetyMode.NONE) {
		ackpinePackageUninstaller.getSessionAsync(ackpineSessionId)
	}

	private lateinit var ackpinePackageUninstaller: PackageUninstallerImpl
	private lateinit var uninstallPackageContract: UninstallContract

	override fun onCreate(savedInstanceState: Bundle?) {
		ackpinePackageUninstaller = PackageUninstaller.getImpl(this)
		super.onCreate(savedInstanceState)
		val packageNameToUninstall = intent.extras?.getString(PACKAGE_NAME_KEY)
		if (packageNameToUninstall == null) {
			completeSessionExceptionally(IllegalStateException("$TAG: packageNameToUninstall was null."))
			finish()
			return
		}
		uninstallPackageContract = UninstallPackageContract(packageNameToUninstall)
		if (savedInstanceState == null) {
			launchUninstallActivity()
		}
	}

	override fun onActivityResult(resultCode: Int) {
		val result = uninstallPackageContract.parseResult(this, resultCode)
		completeSession(result)
	}

	private fun launchUninstallActivity() {
		val intent = uninstallPackageContract.createIntent(this)
		startActivityForResult(intent)
	}

	internal companion object {

		@JvmSynthetic
		internal const val PACKAGE_NAME_KEY = "ACKPINE_UNINSTALLER_PACKAGE_NAME"
	}
}