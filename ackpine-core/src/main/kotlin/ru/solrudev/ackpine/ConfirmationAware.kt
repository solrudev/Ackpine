package ru.solrudev.ackpine

public sealed interface ConfirmationAware {

	/**
	 * A strategy for handling user's confirmation of installation or uninstallation.
	 *
	 * Default strategy is [Confirmation.DEFERRED].
	 */
	public val confirmation: Confirmation

	/**
	 * Data for a high-priority notification which launches confirmation activity.
	 *
	 * Default value is [NotificationData.DEFAULT].
	 *
	 * Ignored when [confirmation] is [Confirmation.IMMEDIATE].
	 */
	public val notificationData: NotificationData
}