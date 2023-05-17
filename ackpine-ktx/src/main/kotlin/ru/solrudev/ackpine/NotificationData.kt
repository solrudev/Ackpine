package ru.solrudev.ackpine

/**
 * Constructs a new instance of [NotificationData].
 */
public inline fun NotificationData(initializer: NotificationData.Builder.() -> Unit): NotificationData {
	return NotificationData.Builder().apply(initializer).build()
}