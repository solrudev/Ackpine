package ru.solrudev.ackpine

/**
 * DSL extension that is used to configure user-facing confirmation for the [Session].
 */
public sealed interface ConfirmationExtension : ConfirmationAware {

	@set:JvmSynthetic
	override var confirmationStrategy: ConfirmationStrategy

	@set:JvmSynthetic
	override var notificationData: NotificationData
}