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

import android.os.Build
import android.os.Bundle
import androidx.annotation.RestrictTo
import ru.solrudev.ackpine.impl.activity.SessionCommitActivity
import ru.solrudev.ackpine.impl.uninstaller.PackageUninstallerImpl
import ru.solrudev.ackpine.uninstaller.UninstallFailure

private const val WAS_ON_TOP_ON_STOP_KEY = "WAS_ON_TOP_ON_STOP"

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal abstract class UninstallActivity protected constructor(
	tag: String
) : SessionCommitActivity<UninstallFailure>(
	tag, abortedStateFailureFactory = UninstallFailure::Aborted
) {

	override val ackpineSessionFuture by lazy(LazyThreadSafetyMode.NONE) {
		ackpinePackageUninstaller.getSessionAsync(ackpineSessionId)
	}

	private var wasOnTopOnStop = false
	private lateinit var ackpinePackageUninstaller: PackageUninstallerImpl

	override fun onCreate(savedInstanceState: Bundle?) {
		ackpinePackageUninstaller = PackageUninstallerImpl.getInstance(this)
		super.onCreate(savedInstanceState)
		if (savedInstanceState != null) {
			wasOnTopOnStop = savedInstanceState.getBoolean(WAS_ON_TOP_ON_STOP_KEY)
		}
	}

	override fun onStop() {
		super.onStop()
		wasOnTopOnStop = isOnTop()
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
			wasOnTopOnStop = isOnTop()
		}
		outState.putBoolean(WAS_ON_TOP_ON_STOP_KEY, wasOnTopOnStop)
	}

	final override fun onActivityResult(resultCode: Int) {
		if (wasOnTopOnStop) {
			// Uninstaller activity is removed when stopped AND hidden, and therefore
			// it sends result (since API 29), so we need to re-launch
			wasOnTopOnStop = false
			launchUninstallActivity()
			return
		}
		processResult(resultCode)
	}

	protected abstract fun processResult(resultCode: Int)
	protected abstract fun launchUninstallActivity()

	@RestrictTo(RestrictTo.Scope.LIBRARY)
	internal companion object {
		@JvmSynthetic
		internal const val EXTRA_PACKAGE_NAME = "ru.solrudev.ackpine.extra.PACKAGE_NAME"
	}
}