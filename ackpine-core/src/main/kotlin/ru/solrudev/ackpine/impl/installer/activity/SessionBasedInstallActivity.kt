/*
 * Copyright (C) 2023-2024 Ilya Fomichev
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

@file:RequiresApi(Build.VERSION_CODES.LOLLIPOP)

package ru.solrudev.ackpine.impl.installer.activity

import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import ru.solrudev.ackpine.impl.installer.activity.helpers.getParcelableCompat
import ru.solrudev.ackpine.impl.session.helpers.commitSession
import ru.solrudev.ackpine.impl.session.helpers.getSessionBasedSessionCommitProgressValue
import ru.solrudev.ackpine.installer.InstallFailure
import ru.solrudev.ackpine.session.Session

private const val CONFIRMATION_TAG = "SessionBasedInstallConfirmationActivity"
private const val LAUNCHER_TAG = "SessionBasedInstallCommitActivity"

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class SessionBasedInstallCommitActivity : InstallActivity(LAUNCHER_TAG, startsActivity = false) {

	private val sessionId by lazy(LazyThreadSafetyMode.NONE) { getSessionId(LAUNCHER_TAG) }

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		if (savedInstanceState == null) {
			packageInstaller.commitSession(applicationContext, sessionId, ackpineSessionId, requestCode)
			finish()
		}
	}
}

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class SessionBasedInstallConfirmationActivity : InstallActivity(CONFIRMATION_TAG, startsActivity = true) {

	private val sessionId by lazy(LazyThreadSafetyMode.NONE) { getSessionId(CONFIRMATION_TAG) }
	private val handler = Handler(Looper.getMainLooper())

	private val deadSessionCompletionRunnable = Runnable {
		withCompletableSession { session ->
			session?.complete(
				Session.State.Failed(InstallFailure.Generic(message = "Session $sessionId is dead."))
			)
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		if (savedInstanceState == null) {
			launchInstallActivity()
		}
	}

	override fun onDestroy() {
		super.onDestroy()
		if (isFinishing) {
			handler.removeCallbacks(deadSessionCompletionRunnable)
		}
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		super.onActivityResult(requestCode, resultCode, data)
		if (requestCode != this.requestCode) {
			return
		}
		val sessionInfo = packageInstaller.getSessionInfo(sessionId)
		// Hacky workaround: progress not going higher than 0.8 means session is dead. This is needed to complete
		// the Ackpine session with failure on reasons which are not handled in PackageInstallerStatusReceiver.
		// For example, "There was a problem parsing the package" error falls under that.
		val isSessionAlive = sessionInfo != null && sessionInfo.progress >= getSessionBasedSessionCommitProgressValue()
		if (!isSessionAlive) {
			setLoading(isLoading = true, delayMillis = 100)
			handler.postDelayed(deadSessionCompletionRunnable, 1000)
		} else {
			finish()
		}
	}

	private fun launchInstallActivity() {
		intent.extras
			?.getParcelableCompat<Intent>(Intent.EXTRA_INTENT)
			?.let { confirmationIntent -> startActivityForResult(confirmationIntent, requestCode) }
		notifySessionCommitted()
	}
}

private val InstallActivity.packageInstaller: PackageInstaller
	get() = packageManager.packageInstaller

private fun InstallActivity.getSessionId(tag: String): Int {
	val sessionId = intent.extras?.getInt(PackageInstaller.EXTRA_SESSION_ID)
	if (sessionId == null) {
		withCompletableSession { session ->
			session?.completeExceptionally(
				IllegalStateException("$tag: sessionId was null.")
			)
		}
	}
	return sessionId ?: -1
}