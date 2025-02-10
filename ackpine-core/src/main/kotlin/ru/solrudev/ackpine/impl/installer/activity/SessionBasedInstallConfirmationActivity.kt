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
import ru.solrudev.ackpine.impl.helpers.getParcelableCompat
import ru.solrudev.ackpine.impl.installer.session.PreapprovalListener
import ru.solrudev.ackpine.impl.installer.session.getSessionBasedSessionCommitProgressValue
import ru.solrudev.ackpine.installer.InstallFailure
import ru.solrudev.ackpine.session.Session

private const val TAG = "SessionBasedInstallConfirmationActivity"
private const val CAN_INSTALL_PACKAGES_KEY = "CAN_INSTALL_PACKAGES"
private const val IS_FIRST_RESUME_KEY = "IS_FIRST_RESUME"
private const val WAS_ON_TOP_ON_START_KEY = "WAS_ON_TOP_ON_START"

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class SessionBasedInstallConfirmationActivity : InstallActivity(TAG) {

	private val sessionId by lazy(LazyThreadSafetyMode.NONE) {
		val sessionId = intent.extras?.getInt(PackageInstaller.EXTRA_SESSION_ID)
		if (sessionId == null) {
			completeSessionExceptionally(IllegalStateException("$TAG: sessionId was null."))
			finish()
		}
		sessionId ?: -1
	}

	private val isPreapproval
		get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
				&& intent.getBooleanExtra(PackageInstaller.EXTRA_PRE_APPROVAL, false)

	private val handler = Handler(Looper.getMainLooper())
	private var canInstallPackages = false
	private var isFirstResume = true
	private var isOnActivityResultCalled = false
	private var wasOnTopOnStart = false

	private val packageInstaller: PackageInstaller
		get() = packageManager.packageInstaller

	private val deadSessionCompletionRunnable = Runnable {
		completeSession(
			Session.State.Failed(
				InstallFailure.Generic(message = "Session $sessionId is dead.")
			)
		)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		if (isPreapproval) {
			withCompletableSession { session ->
				(session as PreapprovalListener).onPreapproval()
			}
		}
		if (savedInstanceState == null) {
			launchInstallActivity()
		} else {
			canInstallPackages = savedInstanceState.getBoolean(CAN_INSTALL_PACKAGES_KEY)
			isFirstResume = savedInstanceState.getBoolean(IS_FIRST_RESUME_KEY)
			wasOnTopOnStart = savedInstanceState.getBoolean(WAS_ON_TOP_ON_START_KEY)
		}
	}

	override fun onStart() {
		super.onStart()
		wasOnTopOnStart = isOnTop()
	}

	override fun onResume() {
		super.onResume()
		when {
			// Activity is freshly created, skip.
			isFirstResume -> isFirstResume = false
			// Activity was recreated and brought to top, but install confirmation from OS was dismissed.
			!isOnActivityResultCalled && wasOnTopOnStart && isSessionStuck() -> abortSession()
		}
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
		outState.putBoolean(WAS_ON_TOP_ON_START_KEY, wasOnTopOnStart)
	}

	override fun onActivityResult(resultCode: Int) {
		isOnActivityResultCalled = true
		val isActivityCancelled = resultCode == RESULT_CANCELED
		val sessionInfo = packageInstaller.getSessionInfo(sessionId)
		val isSessionAlive = sessionInfo != null
		val isSessionStuck = isSessionStuck(sessionInfo)
		val previousCanInstallPackagesValue = canInstallPackages
		canInstallPackages = canInstallPackages()
		val isInstallPermissionStatusChanged = previousCanInstallPackagesValue != canInstallPackages
		// Order of checks is important.
		when {
			// Confirmation is a preapproval on API >= 34.
			isPreapproval -> finish()
			// User hasn't confirmed installation because confirmation activity didn't appear after permission request.
			isSessionStuck && isInstallPermissionStatusChanged && wasOnTopOnStart -> launchInstallActivity()
			// Session proceeded normally.
			// On API 31-32 in case of requireUserAction = false and if _update_ confirmation was dismissed by clicking
			// outside of confirmation dialog, session will stay stuck, unfortunately, because session progress doesn't
			// get updated after successful confirmation, so we have absolutely no way to differentiate between success
			// and stuck session.
			isSessionAlive && !isSessionStuck -> finish()
			// User has cancelled install permission request or hasn't granted permission.
			!canInstallPackages -> abortSession("Install permission denied")
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

	override fun shouldNotifyWhenCommitted() = !isPreapproval

	private fun launchInstallActivity() {
		canInstallPackages = canInstallPackages()
		intent.extras
			?.getParcelableCompat<Intent>(Intent.EXTRA_INTENT)
			?.let(::startActivityForResult)
	}

	private fun isSessionStuck(
		sessionInfo: PackageInstaller.SessionInfo? = packageInstaller.getSessionInfo(sessionId)
	) = sessionInfo != null && sessionInfo.progress < getSessionBasedSessionCommitProgressValue()

	private fun isOnTop(): Boolean {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
			return false
		}
		val activityManager = getSystemService<ActivityManager>() ?: return false
		val appTask = activityManager.appTasks.firstOrNull() ?: return false
		return this::class.java.name == appTask.taskInfo.topActivity?.className
	}

	private fun canInstallPackages() = Build.VERSION.SDK_INT < Build.VERSION_CODES.O
			|| packageManager.canRequestPackageInstalls()
			|| ContextCompat.checkSelfPermission(this, INSTALL_PACKAGES) == PERMISSION_GRANTED
}