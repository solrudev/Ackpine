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
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import ru.solrudev.ackpine.Ackpine
import ru.solrudev.ackpine.impl.helpers.getParcelableCompat
import ru.solrudev.ackpine.impl.helpers.isPackageInstalled
import ru.solrudev.ackpine.impl.helpers.isPackageInstalledOrNotVisible
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.uninstaller.UninstallFailure
import ru.solrudev.ackpine.uninstaller.parameters.UninstallVersion
import ru.solrudev.ackpine.uninstaller.parameters.VersionedUninstallPolicy

private const val TAG = "PackageInstallerBasedUninstallActivity"
private const val ACTION_ABORT_IF_PACKAGE_STILL_INSTALLED = 1

@RestrictTo(RestrictTo.Scope.LIBRARY)
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
internal class PackageInstallerBasedUninstallActivity : UninstallActivity(TAG) {

	private val logger = Ackpine.loggerProvider.withTag(TAG)

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		if (savedInstanceState == null) {
			launchUninstallActivity()
		}
	}

	override fun processResult(resultCode: Int) {
		// Wait for possible result from PackageInstallerStatusReceiver before completing with failure.
		logger.debug(
			"Processing package installer uninstall result for session %s code=%s",
			ackpineSessionId,
			resultCode
		)
		setLoading(isLoading = true, delayMillis = 200)
		runOnWindowFocused(ACTION_ABORT_IF_PACKAGE_STILL_INSTALLED, delayMillis = 400)
	}

	override fun onWindowFocusAction(action: Int) {
		if (action == ACTION_ABORT_IF_PACKAGE_STILL_INSTALLED) {
			abortIfPackageStillInstalled()
		}
	}

	override fun launchUninstallActivity() {
		val packageName = getUninstalledPackageName() ?: return
		if (!shouldProceedWithUninstall(packageName)) {
			completeSession(
				Session.State.Failed(UninstallFailure.Generic("Requested package version is not installed"))
			)
			finish()
			return
		}
		logger.info(
			"Launching uninstall confirmation intent for session %s packageName=%s",
			ackpineSessionId,
			getUninstalledPackageName()
		)
		intent.extras
			?.getParcelableCompat<Intent>(Intent.EXTRA_INTENT)
			?.let(::startActivityForResult)
	}

	private fun abortIfPackageStillInstalled() {
		val packageName = getUninstalledPackageName() ?: return
		val isPackageStillInstalled = if (isExactUnsafeVersionedFallback()) {
			isPackageInstalledOrNotVisible(packageName)
		} else {
			isPackageInstalled(packageName)
		}
		if (isPackageStillInstalled) {
			logger.warn("Package installer uninstall appears aborted for session %s", ackpineSessionId)
			abortSession("Aborted by user")
		}
	}

	private fun getUninstalledPackageName(): String? {
		val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME)
		if (packageName == null) {
			logger.error("Missing package name for session %s", ackpineSessionId)
			completeSessionExceptionally(IllegalStateException("$TAG: packageName was null."))
			finish()
		}
		return packageName
	}

	private fun isExactUnsafeVersionedFallback(): Boolean {
		val version = intent.getLongExtra(EXTRA_PACKAGE_VERSION_CODE, -1L)
		val policy = intent.getIntExtra(EXTRA_VERSIONED_UNINSTALL_POLICY, VersionedUninstallPolicy.STRICT.ordinal)
		return version != UninstallVersion.HIGHEST.code && policy == VersionedUninstallPolicy.ALLOW_NAME_ONLY.ordinal
	}
}