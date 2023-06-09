package ru.solrudev.ackpine.session.parameters

/**
 * Constructs a new instance of [NotificationData].
 */
public inline fun NotificationData(configure: NotificationDataDsl.() -> Unit): NotificationData {
	return NotificationDataDslBuilder().apply(configure).build()
}