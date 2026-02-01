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

package ru.solrudev.ackpine.impl.uninstaller.activity

import android.os.Bundle
import androidx.annotation.RestrictTo

private const val TAG = "IntentBasedUninstallActivity"

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class IntentBasedUninstallActivity : UninstallActivity(TAG) {

	private lateinit var uninstallPackageContract: UninstallContract
	private var isProcessRecreated = false
	private var wasStopped = false

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val packageNameToUninstall = getPackageNameToUninstall()
		if (packageNameToUninstall == null) {
			completeSessionExceptionally(IllegalStateException("$TAG: packageNameToUninstall was null."))
			finish()
			return
		}
		uninstallPackageContract = UninstallPackageContract(packageNameToUninstall)
		if (savedInstanceState == null) {
			launchUninstallActivity()
		} else {
			isProcessRecreated = !savedInstanceState.getBoolean(IS_CONFIG_CHANGE_RECREATION_KEY)
		}
	}

	override fun onStop() {
		super.onStop()
		wasStopped = true
	}

	override fun onActivityResult(resultCode: Int) {
		if ((wasStopped || isProcessRecreated) && wasOnTopOnStart) {
			// Uninstaller activity sends meaningless result and is removed when stopped (since API 29),
			// so we need to re-launch
			wasStopped = false
			isProcessRecreated = false
			launchUninstallActivity()
			return
		}
		val result = uninstallPackageContract.parseResult(this, resultCode)
		completeSession(result)
	}

	private fun launchUninstallActivity() {
		val intent = uninstallPackageContract.createIntent()
		startActivityForResult(intent)
	}

	private fun getPackageNameToUninstall(): String? {
		return intent.extras?.getString(EXTRA_PACKAGE_NAME)
			?: intent.extras?.getString("ACKPINE_UNINSTALLER_PACKAGE_NAME")
	}
}