package ru.solrudev.ackpine

/**
 * Parameters for creating uninstall session.
 */
public class UninstallParameters private constructor(
	public val packageName: String,

	/**
	 * A strategy for handling user's confirmation of installation or uninstallation.
	 *
	 * Default strategy is [ConfirmationStrategy.DEFERRED].
	 */
	public override val confirmationStrategy: ConfirmationStrategy,

	/**
	 * Data for a high-priority notification which launches confirmation activity.
	 *
	 * Default value is [NotificationData.DEFAULT].
	 *
	 * Ignored when [confirmationStrategy] is [ConfirmationStrategy.IMMEDIATE].
	 */
	public override val notificationData: NotificationData
) : ConfirmationAware {

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false
		other as UninstallParameters
		if (packageName != other.packageName) return false
		if (confirmationStrategy != other.confirmationStrategy) return false
		if (notificationData != other.notificationData) return false
		return true
	}

	override fun hashCode(): Int {
		var result = packageName.hashCode()
		result = 31 * result + confirmationStrategy.hashCode()
		result = 31 * result + notificationData.hashCode()
		return result
	}

	override fun toString(): String {
		return "UninstallParameters(packageName=$packageName, confirmationStrategy=$confirmationStrategy, " +
				"notificationData=$notificationData)"
	}

	/**
	 * Builder for [UninstallParameters].
	 */
	@SessionParametersDslMarker
	public class Builder(public val packageName: String) : ConfirmationExtension {

		/**
		 * A strategy for handling user's confirmation of installation or uninstallation.
		 *
		 * Default strategy is [ConfirmationStrategy.DEFERRED].
		 */
		@set:JvmSynthetic
		public override var confirmationStrategy: ConfirmationStrategy = ConfirmationStrategy.DEFERRED

		/**
		 * Data for a high-priority notification which launches confirmation activity.
		 *
		 * Default value is [NotificationData.DEFAULT].
		 *
		 * Ignored when [confirmationStrategy] is [ConfirmationStrategy.IMMEDIATE].
		 */
		@set:JvmSynthetic
		public override var notificationData: NotificationData = NotificationData.DEFAULT

		/**
		 * Sets [InstallParameters.confirmationStrategy].
		 */
		public fun setConfirmationStrategy(confirmationStrategy: ConfirmationStrategy): Builder = apply {
			this.confirmationStrategy = confirmationStrategy
		}

		/**
		 * Sets [InstallParameters.notificationData].
		 */
		public fun setNotificationData(notificationData: NotificationData): Builder = apply {
			this.notificationData = notificationData
		}

		/**
		 * Constructs a new instance of [InstallParameters].
		 */
		public fun build(): UninstallParameters {
			return UninstallParameters(packageName, confirmationStrategy, notificationData)
		}
	}
}