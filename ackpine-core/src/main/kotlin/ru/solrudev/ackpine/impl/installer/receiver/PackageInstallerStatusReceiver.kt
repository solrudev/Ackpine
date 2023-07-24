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

package ru.solrudev.ackpine.impl.installer.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import ru.solrudev.ackpine.helpers.handleResult
import ru.solrudev.ackpine.impl.activity.SessionCommitActivity
import ru.solrudev.ackpine.impl.installer.activity.SessionBasedInstallConfirmationActivity
import ru.solrudev.ackpine.impl.installer.receiver.helpers.getParcelableExtraCompat
import ru.solrudev.ackpine.impl.installer.receiver.helpers.getSerializableExtraCompat
import ru.solrudev.ackpine.impl.session.CompletableSession
import ru.solrudev.ackpine.installer.InstallFailure
import ru.solrudev.ackpine.session.Session
import java.util.UUID
import ru.solrudev.ackpine.installer.PackageInstaller as AckpinePackageInstaller

@RestrictTo(RestrictTo.Scope.LIBRARY)
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
internal class PackageInstallerStatusReceiver : BroadcastReceiver() {

	private lateinit var pendingResult: PendingResult

	@Suppress("UNCHECKED_CAST")
	override fun onReceive(context: Context, intent: Intent) {
		if (intent.action != getAction(context)) {
			return
		}
		pendingResult = goAsync()
		val packageInstaller = AckpinePackageInstaller.getInstance(context)
		val ackpineSessionId = intent.getSerializableExtraCompat<UUID>(SessionCommitActivity.SESSION_ID_KEY)!!
		try {
			packageInstaller.getSessionAsync(ackpineSessionId).handleResult(
				onException = { exception ->
					pendingResult.finish()
					Log.e("InstallerStatusReceiver", null, exception)
				},
				block = { session ->
					handlePackageInstallerStatus(
						session as? CompletableSession<InstallFailure>,
						ackpineSessionId, intent, context
					)
				})
		} catch (t: Throwable) {
			pendingResult.finish()
			Log.e("InstallerStatusReceiver", null, t)
		}
	}

	private fun handlePackageInstallerStatus(
		session: CompletableSession<InstallFailure>?,
		ackpineSessionId: UUID,
		intent: Intent,
		context: Context
	) {
		try {
			val sessionId = intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1)
			val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)
			if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
				val confirmationIntent = intent.getParcelableExtraCompat<Intent>(Intent.EXTRA_INTENT)
				if (confirmationIntent != null) {
					val wrapperIntent = Intent(context, SessionBasedInstallConfirmationActivity::class.java)
						.putExtra(SessionCommitActivity.SESSION_ID_KEY, ackpineSessionId)
						.putExtra(Intent.EXTRA_INTENT, confirmationIntent)
						.putExtra(PackageInstaller.EXTRA_SESSION_ID, sessionId)
						.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
					context.startActivity(wrapperIntent)
				} else {
					session?.completeExceptionally(
						IllegalArgumentException("PackageInstallerStatusReceiver: confirmationIntent was null.")
					)
				}
				return
			}
			val result = if (status == PackageInstaller.STATUS_SUCCESS) {
				Session.State.Succeeded
			} else {
				val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
				val otherPackageName = intent.getStringExtra(PackageInstaller.EXTRA_OTHER_PACKAGE_NAME)
				val storagePath = intent.getStringExtra(PackageInstaller.EXTRA_STORAGE_PATH)
				Session.State.Failed(InstallFailure.fromStatusCode(status, message, otherPackageName, storagePath))
			}
			session?.complete(result)
		} finally {
			pendingResult.finish()
		}
	}

	internal companion object {

		@JvmSynthetic
		internal fun getAction(context: Context) = "${context.packageName}.PACKAGE_INSTALLER_STATUS"
	}
}