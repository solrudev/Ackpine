package ru.solrudev.ackpine.impl.installer.session.helpers

import android.app.Activity
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import ru.solrudev.ackpine.R
import ru.solrudev.ackpine.session.parameters.Confirmation
import ru.solrudev.ackpine.session.parameters.NotificationData
import java.util.concurrent.atomic.AtomicInteger

@get:JvmSynthetic
internal const val INSTALLER_NOTIFICATION_TAG = "ru.solrudev.ackpine.INSTALLER_NOTIFICATION"

@get:JvmSynthetic
internal const val UNINSTALLER_NOTIFICATION_TAG = "ru.solrudev.ackpine.UNINSTALLER_NOTIFICATION"

@get:JvmSynthetic
internal const val INSTALLER_REQUEST_CODE = 247164518

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

private val notificationId = AtomicInteger(18475)

@JvmSynthetic
internal inline fun <reified T : Activity> Context.launchConfirmation(
	confirmation: Confirmation,
	notificationData: NotificationData,
	tag: String,
	requestCode: Int,
	flags: Int,
	putExtra: (Intent) -> Unit
) {
	val intent = Intent(this, T::class.java).also(putExtra)
	when (confirmation) {
		Confirmation.IMMEDIATE -> startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
		Confirmation.DEFERRED -> showNotification(
			PendingIntent.getActivity(this, requestCode, intent, flags),
			notificationData, tag
		)
	}
}

private fun Context.showNotification(
	intent: PendingIntent,
	notificationData: NotificationData,
	notificationTag: String
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
	getSystemService<NotificationManager>()?.notify(notificationTag, notificationId.incrementAndGet(), notification)
}
