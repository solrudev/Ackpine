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
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.core.content.ContextCompat
import ru.solrudev.ackpine.Ackpine
import ru.solrudev.ackpine.helpers.concurrent.handleResult
import ru.solrudev.ackpine.helpers.concurrent.map
import ru.solrudev.ackpine.impl.helpers.getParcelableCompat
import ru.solrudev.ackpine.impl.installer.CommitProgressValueHolder
import ru.solrudev.ackpine.impl.installer.session.PreapprovalListener
import ru.solrudev.ackpine.installer.InstallFailure
import ru.solrudev.ackpine.session.Session

private const val TAG = "SessionBasedInstallConfirmationActivity"
private const val CAN_INSTALL_PACKAGES_KEY = "CAN_INSTALL_PACKAGES"
private const val IS_FIRST_RESUME_KEY = "IS_FIRST_RESUME"
private const val WAS_ON_TOP_ON_START_KEY = "WAS_ON_TOP_ON_START"
private const val IS_ON_ACTIVITY_RESULT_CALLED_KEY = "IS_ON_ACTIVITY_RESULT_CALLED"
private const val PENDING_RESULT_CODE_KEY = "PENDING_RESULT_CODE"
private const val NO_PENDING_RESULT_CODE = Int.MIN_VALUE
private const val ACTION_PROCESS_CONFIRMATION_RESULT = 1
private const val ACTION_CHECK_DISMISSAL = 2
private const val ACTION_DEAD_SESSION_FALLBACK = 3

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class SessionBasedInstallConfirmationActivity : InstallActivity(TAG) {

	private val logger = Ackpine.loggerProvider.withTag(TAG)

	private val sessionId by lazy(LazyThreadSafetyMode.NONE) {
		val sessionId = intent.extras?.getInt(PackageInstaller.EXTRA_SESSION_ID)
		if (sessionId == null) {
			logger.error("Missing native session ID for session %s", ackpineSessionId)
			completeSessionExceptionally(IllegalStateException("$TAG: sessionId was null."))
			finish()
		}
		sessionId ?: -1
	}

	private val isPreapproval
		get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
				&& intent.getBooleanExtra(PackageInstaller.EXTRA_PRE_APPROVAL, false)

	private val executor by lazy(LazyThreadSafetyMode.NONE) {
		ContextCompat.getMainExecutor(this)
	}

	private var canInstallPackages = false
	private var isFirstResume = true
	private var wasOnTopOnStart = false
	private var isOnActivityResultCalled = false
	private var pendingResultCode = NO_PENDING_RESULT_CODE

	private val packageInstaller: PackageInstaller
		get() = packageManager.packageInstaller

	override fun shouldNotifyWhenCommitted() = !isPreapproval

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val isFirstCreate = savedInstanceState == null
		if (!isFirstCreate) {
			canInstallPackages = savedInstanceState.getBoolean(CAN_INSTALL_PACKAGES_KEY)
			isFirstResume = savedInstanceState.getBoolean(IS_FIRST_RESUME_KEY)
			wasOnTopOnStart = savedInstanceState.getBoolean(WAS_ON_TOP_ON_START_KEY)
			isOnActivityResultCalled = savedInstanceState.getBoolean(IS_ON_ACTIVITY_RESULT_CALLED_KEY)
			pendingResultCode = savedInstanceState.getInt(PENDING_RESULT_CODE_KEY, NO_PENDING_RESULT_CODE)
		}
		if (isPreapproval) {
			handlePreapproval(launchConfirmation = isFirstCreate)
			return
		}
		if (isFirstCreate) {
			launchInstallActivity()
		}
	}

	override fun onStart() {
		super.onStart()
		wasOnTopOnStart = isOnTop()
	}

	override fun onResume() {
		super.onResume()
		if (isFirstResume) {
			// Activity is freshly created, skip.
			isFirstResume = false
			return
		}
		val isConfirmationDismissed = !isOnActivityResultCalled && wasOnTopOnStart
		if (isConfirmationDismissed) {
			runOnWindowFocused(ACTION_CHECK_DISMISSAL)
		}
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		outState.putBoolean(CAN_INSTALL_PACKAGES_KEY, canInstallPackages)
		outState.putBoolean(IS_FIRST_RESUME_KEY, isFirstResume)
		outState.putBoolean(WAS_ON_TOP_ON_START_KEY, wasOnTopOnStart)
		outState.putBoolean(IS_ON_ACTIVITY_RESULT_CALLED_KEY, isOnActivityResultCalled)
		outState.putInt(PENDING_RESULT_CODE_KEY, pendingResultCode)
	}

	override fun onActivityResult(resultCode: Int) {
		isOnActivityResultCalled = true
		pendingResultCode = resultCode
		runOnWindowFocused(ACTION_PROCESS_CONFIRMATION_RESULT)
	}

	override fun onWindowFocusAction(action: Int) {
		when (action) {
			ACTION_PROCESS_CONFIRMATION_RESULT -> {
				val resultCode = pendingResultCode
				if (resultCode != NO_PENDING_RESULT_CODE) {
					pendingResultCode = NO_PENDING_RESULT_CODE
					processConfirmationResult(resultCode)
				}
			}

			ACTION_CHECK_DISMISSAL -> checkDismissal()
			ACTION_DEAD_SESSION_FALLBACK -> processDeadSessionFallback()
		}
	}

	private fun processConfirmationResult(resultCode: Int) {
		val isActivityCancelled = resultCode == RESULT_CANCELED
		val previousCanInstallPackagesValue = canInstallPackages
		canInstallPackages = canInstallPackages()
		val isInstallPermissionStatusChanged = previousCanInstallPackagesValue != canInstallPackages
		val sessionInfo = packageInstaller.getSessionInfo(sessionId)
		val isSessionAlive = sessionInfo != null
		logger.debug(
			"Install confirmation finished for session %s resultCode=%s permissionChanged=%s alive=%s",
			ackpineSessionId,
			resultCode,
			isInstallPermissionStatusChanged,
			isSessionAlive
		)
		isSessionStuck(sessionInfo).handleResult(executor) { isSessionStuck ->
			onInstallConfirmationFinished(
				isSessionStuck,
				isInstallPermissionStatusChanged,
				isSessionAlive,
				isActivityCancelled
			)
		}
	}

	private fun checkDismissal() {
		val isConfirmationDismissed = !isOnActivityResultCalled && wasOnTopOnStart
		if (!isConfirmationDismissed) {
			return
		}
		isSessionStuck().handleResult(executor) { isSessionStuck ->
			if (isSessionStuck) {
				// Activity was recreated and brought to top, but install confirmation from OS was dismissed.
				abortSession()
			}
		}
	}

	private fun processDeadSessionFallback() = isSessionStuck().handleResult(executor) { isSessionStuck ->
		if (!isSessionStuck) {
			// Session proceeded normally after timeout.
			finish()
			return@handleResult
		}
		completeSession(
			Session.State.Failed(
				InstallFailure.Generic(message = "Session $sessionId is dead.")
			)
		)
	}

	private fun onInstallConfirmationFinished(
		isSessionStuck: Boolean,
		isInstallPermissionStatusChanged: Boolean,
		isSessionAlive: Boolean,
		isActivityCancelled: Boolean
	) {
		// Order of checks is important.
		when {
			// Confirmation is a preapproval on API >= 34.
			isPreapproval -> {
				logger.info("Finishing preapproval flow for session %s", ackpineSessionId)
				finish()
			}
			// User hasn't confirmed installation because confirmation activity didn't appear after permission request.
			isSessionStuck && isInstallPermissionStatusChanged && wasOnTopOnStart -> {
				logger.info("Relaunching confirmation for stuck session %s after permission change", ackpineSessionId)
				launchInstallActivity()
			}
			// Session proceeded normally.
			// On API 31-32 in case of requireUserAction = false and if _update_ confirmation was dismissed by clicking
			// outside of confirmation dialog, session will stay stuck, unfortunately, because session progress doesn't
			// get updated after successful confirmation, so we have absolutely no way to differentiate between success
			// and stuck session.
			isSessionAlive && !isSessionStuck -> finish()
			// User has cancelled install permission request or hasn't granted permission.
			!canInstallPackages -> {
				logger.warn("Install permission denied for session %s", ackpineSessionId)
				abortSession("Install permission denied")
			}
			// There was some error while installing which is not handled in PackageInstallerStatusReceiver,
			// or session's progress may have been delayed by Play Protect, or session may have completed
			// too quickly.
			else -> {
				if (isSessionAlive && isActivityCancelled) {
					// User may have dismissed confirmation activity.
					logger.warn("Install confirmation dismiss heuristics for session %s", ackpineSessionId)
				}
				// Wait for possible progress/result from PackageInstallerStatusReceiver before completing with failure.
				logger.info("Waiting for delayed dead-session fallback for session %s", ackpineSessionId)
				setLoading(isLoading = true, delayMillis = 100)
				runOnWindowFocused(ACTION_DEAD_SESSION_FALLBACK, delayMillis = 1000)
			}
		}
	}

	private fun handlePreapproval(launchConfirmation: Boolean) = withCompletableSession { session ->
		val canRequestPreapproval = (session as PreapprovalListener).isPreapprovalActive()
		logger.debug(
			"Handling preapproval branch for session %s active=%s launch=%s",
			ackpineSessionId,
			canRequestPreapproval,
			launchConfirmation
		)
		when {
			!canRequestPreapproval -> finish()
			launchConfirmation -> launchInstallActivity()
		}
	}

	private fun launchInstallActivity() {
		canInstallPackages = canInstallPackages()
		logger.info(
			"Launching install confirmation intent for session %s canInstallPackages=%s",
			ackpineSessionId,
			canInstallPackages
		)
		intent.extras
			?.getParcelableCompat<Intent>(Intent.EXTRA_INTENT)
			?.let(::startActivityForResult)
	}

	private fun isSessionStuck(
		sessionInfo: PackageInstaller.SessionInfo? = packageInstaller.getSessionInfo(sessionId)
	) = CommitProgressValueHolder
		.getAsync(this)
		.map { value ->
			logger.debug(
				"Stuck session heuristics for session %s progress=%s threshold=%s",
				ackpineSessionId,
				sessionInfo?.progress,
				value
			)
			sessionInfo != null && sessionInfo.progress < value
		}

	private fun canInstallPackages() = Build.VERSION.SDK_INT < Build.VERSION_CODES.O
			|| packageManager.canRequestPackageInstalls()
			|| ContextCompat.checkSelfPermission(this, INSTALL_PACKAGES) == PERMISSION_GRANTED
}