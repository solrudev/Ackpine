package ru.solrudev.ackpine

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Parameters for creating install session.
 */
public class InstallParameters private constructor(
	public val apks: List<Uri>,
	public val installerType: InstallerType,

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
		other as InstallParameters
		if (apks != other.apks) return false
		if (installerType != other.installerType) return false
		if (confirmationStrategy != other.confirmationStrategy) return false
		if (notificationData != other.notificationData) return false
		return true
	}

	override fun hashCode(): Int {
		var result = apks.hashCode()
		result = 31 * result + installerType.hashCode()
		result = 31 * result + confirmationStrategy.hashCode()
		result = 31 * result + notificationData.hashCode()
		return result
	}

	override fun toString(): String {
		return "InstallParameters(packages=$apks, installerType=$installerType, " +
				"confirmationStrategy=$confirmationStrategy, notificationData=$notificationData)"
	}

	/**
	 * Builder for [InstallParameters].
	 */
	@SessionParametersDslMarker
	public class Builder(baseApk: Uri) : ConfirmationExtension {

		@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
		public val apks: ApkUriList = ApkUriList(baseApk)

		@set:JvmSynthetic
		public var installerType: InstallerType = InstallerType.DEFAULT

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
		 * Adds [apk] to [InstallParameters.apks].
		 */
		@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
		public fun addApk(apk: Uri): Builder = apply {
			this.apks.add(apk)
		}

		/**
		 * Adds [apks] to [InstallParameters.apks].
		 */
		@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
		public fun addApks(apks: Iterable<Uri>): Builder = apply {
			this.apks.addAll(apks)
		}

		/**
		 * Sets [InstallParameters.installerType].
		 */
		public fun setInstallerType(installerType: InstallerType): Builder = apply {
			this.installerType = installerType
		}

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
		@SuppressLint("NewApi")
		public fun build(): InstallParameters {
			return InstallParameters(apks.toList(), installerType, confirmationStrategy, notificationData)
		}
	}
}