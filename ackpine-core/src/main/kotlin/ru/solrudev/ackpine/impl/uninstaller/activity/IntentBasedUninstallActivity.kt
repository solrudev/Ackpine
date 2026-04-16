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

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.annotation.RestrictTo
import ru.solrudev.ackpine.Ackpine
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.uninstaller.UninstallFailure

private const val TAG = "IntentBasedUninstallActivity"

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class IntentBasedUninstallActivity : UninstallActivity(TAG) {

	private val logger = Ackpine.loggerProvider.withTag(TAG)

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		if (savedInstanceState == null) {
			launchUninstallActivity()
		}
	}

	override fun processResult(resultCode: Int) {
		logger.debug("Intent-based uninstall result for session %s code=%s", ackpineSessionId, resultCode)
		val result = when (resultCode) {
			RESULT_OK -> Session.State.Succeeded
			RESULT_CANCELED -> Session.State.Failed(UninstallFailure.Aborted("Session was cancelled"))
			else -> Session.State.Failed(UninstallFailure.Generic())
		}
		completeSession(result)
	}

	@Suppress("DEPRECATION")
	override fun launchUninstallActivity() {
		val packageName = getPackageNameToUninstall() ?: return
		logger.info("Launching intent-based uninstall UI for session %s packageName=%s", ackpineSessionId, packageName)
		val intent = Intent(Intent.ACTION_UNINSTALL_PACKAGE)
			.setData(Uri.parse("package:$packageName"))
			.putExtra(Intent.EXTRA_RETURN_RESULT, true)
		startActivityForResult(intent)
	}

	private fun getPackageNameToUninstall(): String? {
		val packageNameToUninstall = intent.extras?.getString(EXTRA_PACKAGE_NAME)
			?: intent.extras?.getString("ACKPINE_UNINSTALLER_PACKAGE_NAME")
		if (packageNameToUninstall == null) {
			logger.error("Missing package name for session %s", ackpineSessionId)
			completeSessionExceptionally(IllegalStateException("$TAG: packageNameToUninstall was null."))
			finish()
		}
		return packageNameToUninstall
	}
}