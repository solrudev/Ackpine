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

import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import ru.solrudev.ackpine.impl.installer.activity.helpers.getParcelableCompat
import ru.solrudev.ackpine.impl.installer.receiver.PackageInstallerStatusReceiver
import ru.solrudev.ackpine.impl.session.helpers.UPDATE_CURRENT_FLAGS
import ru.solrudev.ackpine.installer.InstallFailure
import ru.solrudev.ackpine.session.Session

private const val CONFIRMATION_TAG = "SessionBasedInstallConfirmationActivity"
private const val LAUNCHER_TAG = "SessionBasedInstallCommitActivity"
private const val CONFIRMATION_REQUEST_CODE = 240126683
private const val RECEIVER_REQUEST_CODE = 951235122

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class SessionBasedInstallCommitActivity : InstallActivity(LAUNCHER_TAG) {

	private val sessionId by lazy { getSessionId(LAUNCHER_TAG) }

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		if (savedInstanceState == null) {
			commitSession()
		}
	}

	private fun commitSession() {
		val receiverIntent = Intent(applicationContext, PackageInstallerStatusReceiver::class.java).apply {
			action = PackageInstallerStatusReceiver.getAction(applicationContext)
			putExtra(SESSION_ID_KEY, ackpineSessionId)
		}
		val receiverPendingIntent = PendingIntent.getBroadcast(
			applicationContext,
			RECEIVER_REQUEST_CODE,
			receiverIntent,
			UPDATE_CURRENT_FLAGS
		)
		val statusReceiver = receiverPendingIntent.intentSender
		// if session doesn't exist, it means user finished this activity,
		// i.e. Ackpine session was prematurely completed with Aborted failure,
		// therefore the session was abandoned
		if (packageInstaller.getSessionInfo(sessionId) != null) {
			packageInstaller.openSession(sessionId).commit(statusReceiver)
			withCompletableSession { session ->
				session?.notifyCommitted()
			}
		}
		finish()
	}
}

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class SessionBasedInstallConfirmationActivity : InstallActivity(CONFIRMATION_TAG, CONFIRMATION_REQUEST_CODE) {

	private val sessionId by lazy { getSessionId(CONFIRMATION_TAG) }

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		if (savedInstanceState == null) {
			launchInstallActivity()
		}
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		super.onActivityResult(requestCode, resultCode, data)
		if (requestCode != CONFIRMATION_REQUEST_CODE) {
			return
		}
		val sessionInfo = packageInstaller.getSessionInfo(sessionId)
		// Hacky workaround: progress not going higher than 0.8 means session is dead. This is needed to complete
		// the Ackpine session with failure on reasons which are not handled in PackageInstallerStatusReceiver.
		// For example, "There was a problem parsing the package" error falls under that.
		val isSessionAlive = sessionInfo != null && sessionInfo.progress >= 0.81
		if (!isSessionAlive) {
			withCompletableSession { session ->
				session?.complete(
					Session.State.Failed(InstallFailure.Generic(message = "Session $sessionId is dead."))
				)
			}
		} else {
			finish()
		}
	}

	private fun launchInstallActivity() {
		intent.extras
			?.getParcelableCompat<Intent>(Intent.EXTRA_INTENT)
			?.let { confirmationIntent -> startActivityForResult(confirmationIntent, CONFIRMATION_REQUEST_CODE) }
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