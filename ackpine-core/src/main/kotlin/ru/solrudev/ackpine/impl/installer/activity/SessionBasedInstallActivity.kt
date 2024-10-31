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

import android.Manifest.permission.INSTALL_PACKAGES
import android.app.ActivityManager
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import ru.solrudev.ackpine.impl.installer.activity.helpers.getParcelableCompat
import ru.solrudev.ackpine.impl.session.helpers.commitSession
import ru.solrudev.ackpine.impl.session.helpers.getSessionBasedSessionCommitProgressValue
import ru.solrudev.ackpine.installer.InstallFailure
import ru.solrudev.ackpine.session.Session

private const val CONFIRMATION_TAG = "SessionBasedInstallConfirmationActivity"
private const val LAUNCHER_TAG = "SessionBasedInstallCommitActivity"
private const val CAN_INSTALL_PACKAGES_KEY = "CAN_INSTALL_PACKAGES"
private const val IS_FIRST_RESUME_KEY = "IS_FIRST_RESUME"

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
	private var canInstallPackages = false
	private var isFirstResume = true
	private var isOnActivityResultCalled = false

	private val deadSessionCompletionRunnable = Runnable {
		withCompletableSession { session ->
			session?.complete(
				Session.State.Failed(InstallFailure.Generic(message = "Session $sessionId is dead."))
			)
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		canInstallPackages = savedInstanceState?.getBoolean(CAN_INSTALL_PACKAGES_KEY) ?: canInstallPackages()
		isFirstResume = savedInstanceState?.getBoolean(IS_FIRST_RESUME_KEY) ?: true
		if (savedInstanceState == null) {
			launchInstallActivity()
		}
	}

	override fun onResume() {
		super.onResume()
		if (!isFirstResume && !isOnActivityResultCalled && getTopActivityClassName() == this::class.java.name) {
			// Activity was recreated and brought to top, but install confirmation from OS was dismissed.
			abortSession()
		}
		isFirstResume = false
	}

	override fun onDestroy() {
		super.onDestroy()
		if (isFinishing) {
			handler.removeCallbacks(deadSessionCompletionRunnable)
		}
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		outState.putBoolean(CAN_INSTALL_PACKAGES_KEY, canInstallPackages)
		outState.putBoolean(IS_FIRST_RESUME_KEY, isFirstResume)
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		if (requestCode != this.requestCode) {
			return
		}
		isOnActivityResultCalled = true
		val isActivityCancelled = resultCode == RESULT_CANCELED
		val sessionInfo = packageInstaller.getSessionInfo(sessionId)
		val isSessionAlive = sessionInfo != null
		val isSessionStuck = sessionInfo != null && sessionInfo.progress < getSessionBasedSessionCommitProgressValue()
		val previousCanInstallPackagesValue = canInstallPackages
		canInstallPackages = canInstallPackages()
		val isInstallPermissionStatusChanged = previousCanInstallPackagesValue != canInstallPackages
		// Order of checks is important.
		when {
			// User has cancelled install permission request or hasn't granted permission.
			!canInstallPackages -> abortSession("Install permission denied")
			// User hasn't confirmed installation because confirmation activity didn't appear after permission request.
			// Unfortunately, on some OS versions session may stay stuck if confirmation was dismissed by clicking
			// outside of confirmation dialog, so this may lead to repeated confirmation if permission status changes.
			isSessionStuck && isInstallPermissionStatusChanged -> launchInstallActivity()
			// Session proceeded normally.
			// On API 31-32 in case of requireUserAction = false and if _update_ confirmation was dismissed by clicking
			// outside of confirmation dialog, session will stay stuck, unfortunately, because for some reason progress
			// gets updated almost like the installation was confirmed even though it wasn't and no result is received
			// from PackageInstallerStatusReceiver.
			isSessionAlive && !isSessionStuck -> finish()
			// User has dismissed confirmation activity.
			isSessionAlive && isActivityCancelled -> abortSession()
			// There was some error while installing which is not handled in PackageInstallerStatusReceiver,
			// or session may have completed too quickly.
			else -> {
				// Wait for possible result from PackageInstallerStatusReceiver before completing with failure.
				setLoading(isLoading = true, delayMillis = 100)
				handler.postDelayed(deadSessionCompletionRunnable, 1000)
			}
		}
	}

	private fun launchInstallActivity() {
		canInstallPackages = canInstallPackages()
		intent.extras
			?.getParcelableCompat<Intent>(Intent.EXTRA_INTENT)
			?.let { confirmationIntent -> startActivityForResult(confirmationIntent, requestCode) }
		notifySessionCommitted()
	}

	private fun getTopActivityClassName(): String? {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
			return null
		}
		return getSystemService<ActivityManager>()
			?.appTasks
			?.firstOrNull()
			?.taskInfo
			?.topActivity
			?.className
	}

	private fun canInstallPackages() = Build.VERSION.SDK_INT < Build.VERSION_CODES.O
			|| packageManager.canRequestPackageInstalls()
			|| ContextCompat.checkSelfPermission(this, INSTALL_PACKAGES) == PERMISSION_GRANTED
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