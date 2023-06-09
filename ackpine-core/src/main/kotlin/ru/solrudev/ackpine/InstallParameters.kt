package ru.solrudev.ackpine

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Parameters for creating install session.
 */
public class InstallParameters private constructor(

	/**
	 * List of APKs [URIs][Uri] to install in one session.
	 */
	public val apks: ApkList,

	/**
	 * Type of the package installer implementation.
	 *
	 * Default value is [InstallerType.DEFAULT].
	 */
	public val installerType: InstallerType,

	/**
	 * A strategy for handling user's confirmation of installation or uninstallation.
	 *
	 * Default strategy is [Confirmation.DEFERRED].
	 */
	public override val confirmation: Confirmation,

	/**
	 * Data for a high-priority notification which launches confirmation activity.
	 *
	 * Default value is [NotificationData.DEFAULT].
	 *
	 * Ignored when [confirmation] is [Confirmation.IMMEDIATE].
	 */
	public override val notificationData: NotificationData
) : ConfirmationAware {

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false
		other as InstallParameters
		if (apks != other.apks) return false
		if (installerType != other.installerType) return false
		if (confirmation != other.confirmation) return false
		if (notificationData != other.notificationData) return false
		return true
	}

	override fun hashCode(): Int {
		var result = apks.hashCode()
		result = 31 * result + installerType.hashCode()
		result = 31 * result + confirmation.hashCode()
		result = 31 * result + notificationData.hashCode()
		return result
	}

	override fun toString(): String {
		return "InstallParameters(apks=$apks, installerType=$installerType, " +
				"confirmation=$confirmation, notificationData=$notificationData)"
	}

	/**
	 * Builder for [InstallParameters].
	 */
	@SessionParametersDsl
	public class Builder : ConfirmationExtension {

		@SuppressLint("NewApi")
		public constructor(baseApk: Uri) {
			this.apks = RealMutableApkList(baseApk)
		}

		@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
		public constructor(apks: Iterable<Uri>) {
			this.apks = RealMutableApkList(apks)
		}

		/**
		 * Mutable list of APKs [URIs][Uri] to install in one session.
		 */
		@get:JvmSynthetic
		@get:JvmName("getMutableApks")
		public val apks: MutableApkList

		/**
		 * Type of the package installer implementation.
		 *
		 * Default value is [InstallerType.DEFAULT].
		 *
		 * When getting/setting the value of this property, the following invariants are taken into account:
		 * * When on API level < 21, [InstallerType.INTENT_BASED] is always returned/set regardless of the
		 * current/provided value;
		 * * When on API level >= 21 and [apks] contain more than one entry, [InstallerType.SESSION_BASED] is always
		 * returned/set regardless of the current/provided value.
		 */
		@set:JvmSynthetic
		public var installerType: InstallerType = InstallerType.DEFAULT
			get() {
				field = applyInstallerTypeInvariants(field)
				return field
			}
			set(value) {
				field = applyInstallerTypeInvariants(value)
			}

		/**
		 * A strategy for handling user's confirmation of installation or uninstallation.
		 *
		 * Default strategy is [Confirmation.DEFERRED].
		 */
		@set:JvmSynthetic
		public override var confirmation: Confirmation = Confirmation.DEFERRED

		/**
		 * Data for a high-priority notification which launches confirmation activity.
		 *
		 * Default value is [NotificationData.DEFAULT].
		 *
		 * Ignored when [confirmation] is [Confirmation.IMMEDIATE].
		 */
		@set:JvmSynthetic
		public override var notificationData: NotificationData = NotificationData.DEFAULT

		/**
		 * List of APKs [URIs][Uri] to install in one session.
		 */
		public fun getApks(): ApkList = apks

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
		 * Sets [InstallParameters.installerType], taking into account the following invariants:
		 * * When on API level < 21, [InstallerType.INTENT_BASED] is always set regardless of the provided value;
		 * * When on API level >= 21 and [apks] contain more than one entry, [InstallerType.SESSION_BASED] is always
		 * set regardless of the provided value.
		 */
		public fun setInstallerType(installerType: InstallerType): Builder = apply {
			this.installerType = installerType
		}

		/**
		 * Sets [InstallParameters.confirmation].
		 */
		public fun setConfirmation(confirmation: Confirmation): Builder = apply {
			this.confirmation = confirmation
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
			return InstallParameters(apks, installerType, confirmation, notificationData)
		}

		private fun applyInstallerTypeInvariants(value: InstallerType) = when {
			!areSplitPackagesSupported() -> InstallerType.INTENT_BASED
			apks.size > 1 && areSplitPackagesSupported() -> InstallerType.SESSION_BASED
			else -> value
		}
	}
}

private class RealMutableApkList : MutableApkList {

	constructor(baseApk: Uri) {
		this.apks = mutableListOf(baseApk)
	}

	@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
	constructor(apks: Iterable<Uri>) {
		checkSplitPackagesSupport()
		require(apks.any()) { "No APKs provided. It's required to have at least one base APK to create a session." }
		this.apks = apks.toMutableList()
	}

	override val size: Int
		get() = apks.size

	private val apks: MutableList<Uri>

	override fun add(apk: Uri) {
		checkSplitPackagesSupport()
		this.apks.add(apk)
	}

	override fun addAll(apks: Iterable<Uri>) {
		checkSplitPackagesSupport()
		this.apks.addAll(apks)
	}

	override fun toList() = apks.toList()
	override fun equals(other: Any?) = apks == other
	override fun hashCode() = apks.hashCode()
	override fun toString() = "ApkList($apks)"

	private fun checkSplitPackagesSupport() {
		if (!areSplitPackagesSupported()) {
			throw SplitPackagesNotSupportedException()
		}
	}
}