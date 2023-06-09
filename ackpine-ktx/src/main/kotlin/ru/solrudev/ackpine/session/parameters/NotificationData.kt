package ru.solrudev.ackpine.session.parameters

/**
 * Constructs a new instance of [NotificationData].
 */
public inline fun NotificationData(initializer: NotificationData.Builder.() -> Unit): NotificationData {
	return NotificationData.Builder().apply(initializer).build()
}