/*
 * Copyright (C) 2023-2025 Ilya Fomichev
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

package ru.solrudev.ackpine.impl.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import com.google.common.util.concurrent.ListenableFuture
import ru.solrudev.ackpine.AckpineThreadPool
import ru.solrudev.ackpine.helpers.concurrent.handleResult
import ru.solrudev.ackpine.impl.activity.SessionCommitActivity
import ru.solrudev.ackpine.impl.database.AckpineDatabase
import ru.solrudev.ackpine.impl.helpers.CANCEL_CURRENT_FLAGS
import ru.solrudev.ackpine.impl.helpers.NotificationIntents
import ru.solrudev.ackpine.impl.helpers.SessionIdIntents
import ru.solrudev.ackpine.impl.helpers.getParcelableExtraCompat
import ru.solrudev.ackpine.impl.helpers.showConfirmationNotification
import ru.solrudev.ackpine.impl.installer.session.PackageInstallerStatus
import ru.solrudev.ackpine.impl.installer.session.PreapprovalListener
import ru.solrudev.ackpine.impl.session.CompletableSession
import ru.solrudev.ackpine.installer.InstallFailure
import ru.solrudev.ackpine.session.Failure
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.session.parameters.Confirmation
import java.util.UUID
import kotlin.random.Random
import kotlin.random.nextInt

@RestrictTo(RestrictTo.Scope.LIBRARY)
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
internal abstract class SystemPackageInstallerStatusReceiver<F : Failure> protected constructor(
	private val confirmationWrapperActivityClass: Class<out SessionCommitActivity<F>>,
	private val tag: String
) : BroadcastReceiver() {

	abstract fun getAckpineSessionAsync(
		context: Context,
		ackpineSessionId: UUID
	): ListenableFuture<out CompletableSession<F>?>

	abstract fun getFailure(status: Int, message: String?, otherPackageName: String?, storagePath: String?): F
	abstract fun getAction(context: Context): String
	open fun modifyConfirmationWrapperIntent(intent: Intent, wrapperIntent: Intent): Intent = wrapperIntent

	final override fun onReceive(context: Context, intent: Intent) {
		if (intent.action != getAction(context)) {
			return
		}
		val pendingResult = goAsync()
		val ackpineSessionId = SessionIdIntents.getSessionId(intent, tag)
		getAckpineSessionAsync(context, ackpineSessionId).handleResult(
			onException = { exception ->
				pendingResult.finish()
				Log.e(tag, null, exception)
			},
			block = { session ->
				handlePackageInstallerStatus(session, ackpineSessionId, intent, context, pendingResult)
			})
	}

	private fun handlePackageInstallerStatus(
		session: CompletableSession<F>?,
		ackpineSessionId: UUID,
		intent: Intent,
		context: Context,
		pendingResult: PendingResult
	) {
		try {
			val sessionId = intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1)
			val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)
			val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
			val legacyStatus = intent.getIntExtra(EXTRA_LEGACY_STATUS, -1)
			val packageInstallerStatus = PackageInstallerStatus.fromLegacyStatus(legacyStatus)
			val isPreapproval = isPreapproval(intent) || packageInstallerStatus?.isPreapproval == true
			when {
				status == PackageInstaller.STATUS_PENDING_USER_ACTION -> {
					val confirmationIntent = intent.getParcelableExtraCompat<Intent>(Intent.EXTRA_INTENT)
					if (confirmationIntent == null) {
						session?.completeExceptionally(
							IllegalStateException("$tag: confirmationIntent was null.")
						)
						return
					}
					if (Build.VERSION.SDK_INT in 31..32 && !getRequireUserAction(intent)) {
						setConfirmationLaunched(context, ackpineSessionId)
					}
					handleConfirmation(
						context, intent, confirmationIntent, ackpineSessionId, sessionId, isPreapproval
					)
					return
				}

				isPreapproval -> handlePreapprovalResult(session, status, packageInstallerStatus, message, intent)
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
		val wrapperIntent = Intent(context, confirmationWrapperActivityClass)
			.putExtra(Intent.EXTRA_INTENT, confirmationIntent)
			.putExtra(PackageInstaller.EXTRA_SESSION_ID, sessionId)
			.let { wrapperIntent -> modifyConfirmationWrapperIntent(intent, wrapperIntent) }
		SessionIdIntents.putSessionId(wrapperIntent, ackpineSessionId)
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
		session: CompletableSession<F>?,
		status: Int,
		message: String?,
		intent: Intent
	) {
		val result = when (status) {
			PackageInstaller.STATUS_SUCCESS -> Session.State.Succeeded
			else -> Session.State.Failed(getFailure(intent, status, message))
		}
		session?.complete(result)
	}

	private fun handlePreapprovalResult(
		session: CompletableSession<F>?,
		status: Int,
		packageInstallerStatus: PackageInstallerStatus?,
		message: String?,
		intent: Intent
	) {
		session as PreapprovalListener
		when (status) {
			PackageInstaller.STATUS_SUCCESS -> session.onPreapprovalSucceeded()
			else -> {
				val publicFailure = getFailure(intent, status, message) as InstallFailure
				session.onPreapprovalFailed(packageInstallerStatus, publicFailure)
			}
		}
	}

	private fun showConfirmationNotification(
		intent: Intent,
		confirmationIntent: Intent,
		context: Context,
		ackpineSessionId: UUID
	) {
		val notificationId = intent.getIntExtra(NotificationIntents.EXTRA_NOTIFICATION_ID, 0)
		val notificationData = NotificationIntents.getNotificationData(intent, tag)
		context.showConfirmationNotification(
			confirmationIntent,
			notificationData,
			ackpineSessionId, notificationId,
			generateRequestCode(),
			CANCEL_CURRENT_FLAGS
		)
	}

	private fun setConfirmationLaunched(
		context: Context,
		ackpineSessionId: UUID
	) = AckpineThreadPool.execute {
		AckpineDatabase
			.getInstance(context, AckpineThreadPool)
			.confirmationLaunchDao()
			.setConfirmationLaunched(ackpineSessionId.toString())
	}

	private fun isPreapproval(intent: Intent) = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
			&& intent.getBooleanExtra(PackageInstaller.EXTRA_PRE_APPROVAL, false)

	private fun getRequireUserAction(intent: Intent) = intent.getBooleanExtra(EXTRA_REQUIRE_USER_ACTION, true)

	private fun getFailure(intent: Intent, status: Int, message: String?): F {
		val otherPackageName = intent.getStringExtra(PackageInstaller.EXTRA_OTHER_PACKAGE_NAME)
		val storagePath = intent.getStringExtra(PackageInstaller.EXTRA_STORAGE_PATH)
		return getFailure(status, message, otherPackageName, storagePath)
	}

	private fun generateRequestCode() = Random.nextInt(10000..1000000)

	@RestrictTo(RestrictTo.Scope.LIBRARY)
	internal companion object {

		@JvmSynthetic
		internal const val EXTRA_CONFIRMATION = "ru.solrudev.ackpine.extra.CONFIRMATION"

		@JvmSynthetic
		internal const val EXTRA_REQUIRE_USER_ACTION = "ru.solrudev.ackpine.extra.REQUIRE_USER_ACTION"

		// https://cs.android.com/android/platform/superproject/main/+/main:frameworks/base/core/java/android/content/pm/PackageInstaller.java;drc=61197364367c9e404c7da6900658f1b16c42d0da;l=331
		/**
		 * The status as used internally in the package manager. Refer to [PackageInstallerStatus]
		 * for a list of all valid legacy statuses.
		 */
		private const val EXTRA_LEGACY_STATUS = "android.content.pm.extra.LEGACY_STATUS"
	}
}