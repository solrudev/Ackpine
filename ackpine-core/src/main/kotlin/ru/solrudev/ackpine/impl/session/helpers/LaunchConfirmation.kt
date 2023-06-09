package ru.solrudev.ackpine.impl.session.helpers

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import ru.solrudev.ackpine.R
import ru.solrudev.ackpine.impl.activity.SessionCommitActivity
import ru.solrudev.ackpine.session.parameters.Confirmation
import ru.solrudev.ackpine.session.parameters.NotificationData
import java.util.UUID

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

@JvmSynthetic
internal inline fun <reified T : SessionCommitActivity<*, *>> Context.launchConfirmation(
	confirmation: Confirmation,
	notificationData: NotificationData,
	sessionId: UUID,
	tag: String,
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
			notificationData, tag, notificationId
		)
	}
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
		setSmallIcon(notificationData.icon)
		setOngoing(true)
		setFullScreenIntent(intent, true)
		setAutoCancel(true)
	}.build()
	getSystemService<NotificationManager>()?.notify(notificationTag, notificationId, notification)
}
