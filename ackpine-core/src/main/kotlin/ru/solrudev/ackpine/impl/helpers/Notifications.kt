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

package ru.solrudev.ackpine.impl.helpers

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import androidx.core.os.bundleOf
import ru.solrudev.ackpine.core.R
import ru.solrudev.ackpine.impl.activity.SessionCommitActivity
import ru.solrudev.ackpine.impl.installer.activity.helpers.getSerializableCompat
import ru.solrudev.ackpine.impl.installer.receiver.helpers.getParcelableExtraCompat
import ru.solrudev.ackpine.resources.ResolvableString
import ru.solrudev.ackpine.session.parameters.Confirmation
import ru.solrudev.ackpine.session.parameters.DrawableId
import ru.solrudev.ackpine.session.parameters.NotificationData
import java.util.UUID

@JvmSynthetic
@JvmField
internal val CANCEL_CURRENT_FLAGS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
	PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
} else {
	PendingIntent.FLAG_CANCEL_CURRENT
}

@JvmSynthetic
@JvmField
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
		.putExtra(SessionCommitActivity.EXTRA_ACKPINE_SESSION_ID, sessionId)
		.also(putExtra)
	when (confirmation) {
		Confirmation.IMMEDIATE -> startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
		Confirmation.DEFERRED -> showConfirmationNotification(
			intent, notificationData, sessionId, notificationId, requestCode, flags
		)
	}
}

/**
 * Shows a notification which launches session's confirmation.
 */
@JvmSynthetic
internal fun Context.showConfirmationNotification(
	intent: Intent,
	notificationData: NotificationData,
	sessionId: UUID,
	notificationId: Int,
	requestCode: Int,
	flags: Int
) = showNotification(
	PendingIntent.getActivity(this, requestCode, intent, flags),
	notificationData, sessionId.toString(), notificationId
)

private fun Context.showNotification(
	intent: PendingIntent,
	notificationData: NotificationData,
	notificationTag: String,
	notificationId: Int
) {
	val context = this
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
		setSmallIcon(notificationData.icon.drawableId())
		setOngoing(true)
		setAutoCancel(true)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
			val deleteIntent = Intent(context, NotificationDismissalReceiver::class.java)
				.putExtra(Intent.EXTRA_INTENT, intent)
				.putExtra(NotificationDismissalReceiver.EXTRA_NOTIFICATION_TAG, notificationTag)
			NotificationIntents.putNotification(deleteIntent, notificationData, notificationId)
			val deletePendingIntent = PendingIntent.getBroadcast(
				context, notificationId, deleteIntent, CANCEL_CURRENT_FLAGS
			)
			setDeleteIntent(deletePendingIntent)
		}
	}.build()
	getSystemService<NotificationManager>()?.notify(notificationTag, notificationId, notification)
}

@RestrictTo(RestrictTo.Scope.LIBRARY)
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
internal class NotificationDismissalReceiver : BroadcastReceiver() {

	override fun onReceive(context: Context, intent: Intent) {
		val pendingIntent = requireNotNull(intent.getParcelableExtraCompat<PendingIntent>(Intent.EXTRA_INTENT)) {
			"$TAG: pendingIntent was null"
		}
		val notificationData = NotificationIntents.getNotificationData(intent, TAG)
		val notificationTag = requireNotNull(intent.getStringExtra(EXTRA_NOTIFICATION_TAG)) {
			"$TAG: notificationTag was null"
		}
		val notificationId = intent.getIntExtra(NotificationIntents.EXTRA_NOTIFICATION_ID, 0)
		context.showNotification(pendingIntent, notificationData, notificationTag, notificationId)
	}

	internal companion object {

		@JvmSynthetic
		const val EXTRA_NOTIFICATION_TAG = "ru.solrudev.ackpine.extra.NOTIFICATION_TAG"

		private const val TAG = "NotificationDismissalReceiver"
	}
}

internal object NotificationIntents {

	@JvmSynthetic
	internal const val EXTRA_NOTIFICATION_ID = "ru.solrudev.ackpine.extra.NOTIFICATION_ID"

	private const val EXTRA_NOTIFICATION_BUNDLE = "ru.solrudev.ackpine.extra.NOTIFICATION_BUNDLE"
	private const val EXTRA_NOTIFICATION_TITLE = "ru.solrudev.ackpine.extra.NOTIFICATION_TITLE"
	private const val EXTRA_NOTIFICATION_MESSAGE = "ru.solrudev.ackpine.extra.NOTIFICATION_MESSAGE"
	private const val EXTRA_NOTIFICATION_ICON = "ru.solrudev.ackpine.extra.NOTIFICATION_ICON"

	@JvmSynthetic
	internal fun getNotificationData(intent: Intent, tag: String): NotificationData {
		val bundle = intent.getBundleExtra(EXTRA_NOTIFICATION_BUNDLE)!!
		val notificationTitle = requireNotNull(
			bundle.getSerializableCompat<ResolvableString>(EXTRA_NOTIFICATION_TITLE)
		) { "$tag: notificationTitle was null." }
		val notificationMessage = requireNotNull(
			bundle.getSerializableCompat<ResolvableString>(EXTRA_NOTIFICATION_MESSAGE)
		) { "$tag: notificationMessage was null." }
		val notificationIcon = requireNotNull(
			bundle.getSerializableCompat<DrawableId>(EXTRA_NOTIFICATION_ICON)
		) { "$tag: notificationIcon was null." }
		return NotificationData.Builder()
			.setTitle(notificationTitle)
			.setContentText(notificationMessage)
			.setIcon(notificationIcon)
			.build()
	}

	@JvmSynthetic
	internal fun putNotification(intent: Intent, notificationData: NotificationData, notificationId: Int) {
		intent.putExtra(EXTRA_NOTIFICATION_ID, notificationId)
		val notificationBundle = bundleOf(
			EXTRA_NOTIFICATION_TITLE to notificationData.title,
			EXTRA_NOTIFICATION_MESSAGE to notificationData.contentText,
			EXTRA_NOTIFICATION_ICON to notificationData.icon,
		)
		intent.putExtra(EXTRA_NOTIFICATION_BUNDLE, notificationBundle)
	}
}