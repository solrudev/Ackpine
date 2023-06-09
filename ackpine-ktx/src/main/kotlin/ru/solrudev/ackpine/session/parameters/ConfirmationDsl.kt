package ru.solrudev.ackpine.session.parameters

import ru.solrudev.ackpine.session.Session

/**
 * DSL allowing to configure user-facing confirmation for the [Session].
 */
public interface ConfirmationDsl : ConfirmationAware {
	override var confirmation: Confirmation
	override var notificationData: NotificationData
}

/**
 * Configures [notification DSL][NotificationDataDsl].
 */
public inline fun ConfirmationDsl.notification(configure: NotificationDataDsl.() -> Unit) {
	this.notificationData = NotificationData(configure)
}