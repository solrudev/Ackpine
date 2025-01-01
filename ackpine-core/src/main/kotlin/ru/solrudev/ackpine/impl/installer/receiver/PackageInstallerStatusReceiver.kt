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
import ru.solrudev.ackpine.helpers.concurrent.handleResult
import ru.solrudev.ackpine.impl.activity.SessionCommitActivity
import ru.solrudev.ackpine.impl.installer.activity.SessionBasedInstallConfirmationActivity
import ru.solrudev.ackpine.impl.installer.activity.helpers.getSerializableCompat
import ru.solrudev.ackpine.impl.installer.receiver.helpers.getParcelableExtraCompat
import ru.solrudev.ackpine.impl.installer.receiver.helpers.getSerializableExtraCompat
import ru.solrudev.ackpine.impl.installer.session.PreapprovalListener
import ru.solrudev.ackpine.impl.session.CompletableSession
import ru.solrudev.ackpine.impl.session.helpers.CANCEL_CURRENT_FLAGS
import ru.solrudev.ackpine.impl.session.helpers.showConfirmationNotification
import ru.solrudev.ackpine.installer.InstallFailure
import ru.solrudev.ackpine.resources.ResolvableString
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.session.parameters.Confirmation
import ru.solrudev.ackpine.session.parameters.DrawableId
import ru.solrudev.ackpine.session.parameters.NotificationData
import java.util.UUID
import kotlin.random.Random
import kotlin.random.nextInt
import ru.solrudev.ackpine.installer.PackageInstaller as AckpinePackageInstaller

private const val TAG = "PackageInstallerStatusReceiver"

@RestrictTo(RestrictTo.Scope.LIBRARY)
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
internal class PackageInstallerStatusReceiver : BroadcastReceiver() {

	@Suppress("UNCHECKED_CAST")
	override fun onReceive(context: Context, intent: Intent) {
		if (intent.action != getAction(context)) {
			return
		}
		val pendingResult = goAsync()
		val packageInstaller = AckpinePackageInstaller.getInstance(context)
		val ackpineSessionId = intent.getSerializableExtraCompat<UUID>(SessionCommitActivity.EXTRA_ACKPINE_SESSION_ID)!!
		try {
			packageInstaller.getSessionAsync(ackpineSessionId).handleResult(
				onException = { exception ->
					pendingResult.finish()
					Log.e("InstallerStatusReceiver", null, exception)
				},
				block = { session ->
					handlePackageInstallerStatus(
						session as? CompletableSession<InstallFailure>,
						ackpineSessionId, intent, context, pendingResult
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
		context: Context,
		pendingResult: PendingResult
	) {
		try {
			val sessionId = intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1)
			val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)
			val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
			val isPreapproval = isPreapproval(intent)
			when {
				status == PackageInstaller.STATUS_PENDING_USER_ACTION -> {
					val confirmationIntent = intent.getParcelableExtraCompat<Intent>(Intent.EXTRA_INTENT)
					if (confirmationIntent == null) {
						session?.completeExceptionally(
							IllegalArgumentException("$TAG: confirmationIntent was null.")
						)
						return
					}
					handleConfirmation(
						context, intent, confirmationIntent, ackpineSessionId, sessionId, isPreapproval
					)
					return
				}

				isPreapproval -> handlePreapprovalResult(session, status, message, intent)
				else -> handleSessionResult(session, status, message, intent)
			}
		} finally {
			pendingResult.finish()
		}
	}

	private fun handleConfirmation(
		context: Context,
		intent: Intent,
		confirmationIntent: Intent,
		ackpineSessionId: UUID,
		sessionId: Int,
		isPreapproval: Boolean
	) {
		val confirmation = Confirmation.entries[
			intent.getIntExtra(EXTRA_CONFIRMATION, Confirmation.DEFERRED.ordinal)
		]
		val wrapperIntent = Intent(context, SessionBasedInstallConfirmationActivity::class.java)
			.putExtra(SessionCommitActivity.EXTRA_ACKPINE_SESSION_ID, ackpineSessionId)
			.putExtra(Intent.EXTRA_INTENT, confirmationIntent)
			.putExtra(PackageInstaller.EXTRA_SESSION_ID, sessionId)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
			wrapperIntent.putExtra(PackageInstaller.EXTRA_PRE_APPROVAL, isPreapproval)
		}
		when (confirmation) {
			Confirmation.IMMEDIATE -> context.startActivity(
				wrapperIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
			)

			Confirmation.DEFERRED -> showConfirmationNotification(
				intent, wrapperIntent, context, ackpineSessionId
			)
		}
	}

	private fun handleSessionResult(
		session: CompletableSession<InstallFailure>?,
		status: Int,
		message: String?,
		intent: Intent
	) {
		val result = when (status) {
			PackageInstaller.STATUS_SUCCESS -> Session.State.Succeeded
			else -> Session.State.Failed(InstallFailure.fromIntent(intent, status, message))
		}
		session?.complete(result)
	}

	private fun handlePreapprovalResult(
		session: CompletableSession<InstallFailure>?,
		status: Int,
		message: String?,
		intent: Intent
	) {
		session as PreapprovalListener
		when (status) {
			PackageInstaller.STATUS_SUCCESS -> session.onPreapproved()
			else -> session.complete(
				Session.State.Failed(InstallFailure.fromIntent(intent, status, message))
			)
		}
	}

	private fun showConfirmationNotification(
		intent: Intent,
		confirmationIntent: Intent,
		context: Context,
		ackpineSessionId: UUID
	) {
		val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
		val notificationData = getNotificationData(intent)
		context.showConfirmationNotification(
			confirmationIntent,
			notificationData,
			ackpineSessionId, notificationId,
			generateRequestCode(),
			CANCEL_CURRENT_FLAGS
		)
	}

	private fun getNotificationData(intent: Intent): NotificationData {
		val bundle = intent.getBundleExtra(EXTRA_NOTIFICATION_BUNDLE)!!
		val notificationTitle = requireNotNull(
			bundle.getSerializableCompat<ResolvableString>(EXTRA_NOTIFICATION_TITLE)
		) { "$TAG: notificationTitle was null." }
		val notificationMessage = requireNotNull(
			bundle.getSerializableCompat<ResolvableString>(EXTRA_NOTIFICATION_MESSAGE)
		) { "$TAG: notificationMessage was null." }
		val notificationIcon = requireNotNull(
			bundle.getSerializableCompat<DrawableId>(EXTRA_NOTIFICATION_ICON)
		) { "$TAG: notificationIcon was null." }
		return NotificationData.Builder()
			.setTitle(notificationTitle)
			.setContentText(notificationMessage)
			.setIcon(notificationIcon)
			.build()
	}

	private fun isPreapproval(intent: Intent) = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
			&& intent.getBooleanExtra(PackageInstaller.EXTRA_PRE_APPROVAL, false)

	private fun InstallFailure.Companion.fromIntent(intent: Intent, status: Int, message: String?): InstallFailure {
		val otherPackageName = intent.getStringExtra(PackageInstaller.EXTRA_OTHER_PACKAGE_NAME)
		val storagePath = intent.getStringExtra(PackageInstaller.EXTRA_STORAGE_PATH)
		return fromStatusCode(status, message, otherPackageName, storagePath)
	}

	private fun generateRequestCode() = Random.nextInt(10000..1000000)

	internal companion object {

		@JvmSynthetic
		internal fun getAction(context: Context) = "${context.packageName}.PACKAGE_INSTALLER_STATUS"

		@get:JvmSynthetic
		internal const val EXTRA_NOTIFICATION_BUNDLE = "ru.solrudev.ackpine.extra.NOTIFICATION_BUNDLE"

		@get:JvmSynthetic
		internal const val EXTRA_NOTIFICATION_ID = "ru.solrudev.ackpine.extra.NOTIFICATION_ID"

		@get:JvmSynthetic
		internal const val EXTRA_NOTIFICATION_TITLE = "ru.solrudev.ackpine.extra.NOTIFICATION_TITLE"

		@get:JvmSynthetic
		internal const val EXTRA_NOTIFICATION_MESSAGE = "ru.solrudev.ackpine.extra.NOTIFICATION_MESSAGE"

		@get:JvmSynthetic
		internal const val EXTRA_NOTIFICATION_ICON = "ru.solrudev.ackpine.extra.NOTIFICATION_ICON"

		@get:JvmSynthetic
		internal const val EXTRA_CONFIRMATION = "ru.solrudev.ackpine.extra.CONFIRMATION"
	}
}