package ru.solrudev.ackpine

/**
 * Configures the notification data of [confirmation][ConfirmationExtension] extension.
 */
public inline fun ConfirmationExtension.notification(initializer: NotificationData.Builder.() -> Unit) {
	this.notificationData = NotificationData(initializer)
}