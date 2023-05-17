package ru.solrudev.ackpine

public sealed interface ConfirmationAware {

	/**
	 * A strategy for handling user's confirmation of installation or uninstallation.
	 *
	 * Default strategy is [ConfirmationStrategy.DEFERRED].
	 */
	public val confirmationStrategy: ConfirmationStrategy

	/**
	 * Data for a high-priority notification which launches confirmation activity.
	 *
	 * Default value is [NotificationData.DEFAULT].
	 *
	 * Ignored when [confirmationStrategy] is [ConfirmationStrategy.IMMEDIATE].
	 */
	public val notificationData: NotificationData
}