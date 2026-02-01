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
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import ru.solrudev.ackpine.impl.helpers.getParcelableCompat

private const val TAG = "PackageInstallerBasedUninstallActivity"

@RestrictTo(RestrictTo.Scope.LIBRARY)
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
internal class PackageInstallerBasedUninstallActivity : UninstallActivity(TAG) {

	private val handler = Handler(Looper.getMainLooper())
	private var isProcessRecreated = false
	private var wasStopped = false

	private val abortedSessionRunnable = Runnable {
		val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME)
			?: error("$TAG: packageName was null")
		if (isPackageInstalled(packageName)) {
			abortSession("Aborted by user")
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
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
		// Wait for possible result from PackageInstallerStatusReceiver before completing with failure.
		setLoading(isLoading = true, delayMillis = 200)
		handler.postDelayed(abortedSessionRunnable, 400)
	}

	override fun onDestroy() {
		super.onDestroy()
		if (isFinishing) {
			handler.removeCallbacks(abortedSessionRunnable)
		}
	}

	private fun launchUninstallActivity() {
		intent.extras
			?.getParcelableCompat<Intent>(Intent.EXTRA_INTENT)
			?.let(::startActivityForResult)
	}
}