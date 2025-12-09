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
import ru.solrudev.ackpine.impl.uninstaller.UninstallStatusReceiver

@RestrictTo(RestrictTo.Scope.LIBRARY)
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
internal class PackageInstallerBasedUninstallActivity : UninstallActivity(
	tag = "PackageInstallerBasedUninstallActivity"
) {

	private val handler = Handler(Looper.getMainLooper())

	private val abortedSessionRunnable = Runnable {
		val packageName = intent.getStringExtra(UninstallStatusReceiver.EXTRA_PACKAGE_NAME)
			?: error("PackageInstallerBasedUninstallActivity: packageName was null")
		if (isPackageInstalled(packageName)) {
			abortSession("Aborted by user")
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		if (savedInstanceState == null) {
			intent.extras
				?.getParcelableCompat<Intent>(Intent.EXTRA_INTENT)
				?.let(::startActivityForResult)
		}
	}

	override fun onActivityResult(resultCode: Int) {
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
}