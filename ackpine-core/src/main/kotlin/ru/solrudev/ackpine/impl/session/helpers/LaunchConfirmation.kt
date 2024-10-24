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

package ru.solrudev.ackpine.impl.session.helpers

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import androidx.core.content.getSystemService
import ru.solrudev.ackpine.core.R
import ru.solrudev.ackpine.impl.activity.SessionCommitActivity
import ru.solrudev.ackpine.impl.installer.receiver.PackageInstallerStatusReceiver
import ru.solrudev.ackpine.session.parameters.Confirmation
import ru.solrudev.ackpine.session.parameters.NotificationData
import java.util.UUID

private const val ACKPINE_SESSION_BASED_INSTALLER = "ackpine_session_based_installer"
private const val SESSION_COMMIT_PROGRESS_VALUE = "session_commit_progress_value"

@get:JvmSynthetic
internal val CANCEL_CURRENT_FLAGS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
	PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
} else {
	PendingIntent.FLAG_CANCEL_CURRENT
}

@get:JvmSynthetic
internal val UPDATE_CURRENT_FLAGS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
	PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
} else {
	PendingIntent.FLAG_UPDATE_CURRENT
}

/**
 * Launches session's confirmation with regards to provided [confirmation] mode.
 */
@JvmSynthetic
internal inline fun <reified T : SessionCommitActivity<*>> Context.launchConfirmation(
	confirmation: Confirmation,
	notificationData: NotificationData,
	sessionId: UUID,
	notificationId: Int,
	requestCode: Int,
	flags: Int,
	putExtra: (Intent) -> Unit
) {
	val intent = Intent(this, T::class.java)
		.putExtra(SessionCommitActivity.SESSION_ID_KEY, sessionId)
		.also(putExtra)
	when (confirmation) {
		Confirmation.IMMEDIATE -> startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
		Confirmation.DEFERRED -> showNotification(
			PendingIntent.getActivity(this, requestCode, intent, flags),
			notificationData, sessionId.toString(), notificationId
		)
	}
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
@JvmSynthetic
internal fun PackageInstaller.commitSession(
	context: Context,
	sessionId: Int,
	ackpineSessionId: UUID,
	requestCode: Int
) {
	val receiverIntent = Intent(context, PackageInstallerStatusReceiver::class.java).apply {
		action = PackageInstallerStatusReceiver.getAction(context)
		putExtra(SessionCommitActivity.SESSION_ID_KEY, ackpineSessionId)
	}
	val receiverPendingIntent = PendingIntent.getBroadcast(context, requestCode, receiverIntent, UPDATE_CURRENT_FLAGS)
	val statusReceiver = receiverPendingIntent.intentSender
	if (getSessionInfo(sessionId) != null) {
		openSession(sessionId).commit(statusReceiver)
		val preferences = context.getSharedPreferences(ACKPINE_SESSION_BASED_INSTALLER, MODE_PRIVATE)
		if (!preferences.contains(SESSION_COMMIT_PROGRESS_VALUE)) {
			preferences.edit {
				putFloat(SESSION_COMMIT_PROGRESS_VALUE, getSessionInfo(sessionId)!!.progress + 0.01f)
			}
		}
	}
}

@JvmSynthetic
internal fun Context.getSessionBasedSessionCommitProgressValue(): Float {
	return getSharedPreferences(ACKPINE_SESSION_BASED_INSTALLER, MODE_PRIVATE)
		.getFloat(SESSION_COMMIT_PROGRESS_VALUE, 1f)
}

private fun Context.showNotification(
	intent: PendingIntent,
	notificationData: NotificationData,
	notificationTag: String,
	notificationId: Int
) {
	val channelId = getString(R.string.ackpine_notification_channel_id)
	val title = notificationData.title.resolve(this)
	val contentText = notificationData.contentText.resolve(this)
	val notification = NotificationCompat.Builder(this, channelId).apply {
		setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
		setContentTitle(title)
		setContentText(contentText)
		if (contentText.length > 48) {
			setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
		}
		setContentIntent(intent)
		priority = NotificationCompat.PRIORITY_MAX
		setDefaults(NotificationCompat.DEFAULT_ALL)
		setSmallIcon(notificationData.icon.drawableId)
		setOngoing(true)
		setAutoCancel(true)
	}.build()
	getSystemService<NotificationManager>()?.notify(notificationTag, notificationId, notification)
}