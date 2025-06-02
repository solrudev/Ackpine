/*
 * Copyright (C) 2023 Ilya Fomichev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.solrudev.ackpine.installer.parameters

import android.annotation.SuppressLint
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import ru.solrudev.ackpine.DelicateAckpineApi
import ru.solrudev.ackpine.exceptions.SplitPackagesNotSupportedException
import ru.solrudev.ackpine.plugability.AckpinePlugin
import ru.solrudev.ackpine.plugability.AckpinePluginContainer
import ru.solrudev.ackpine.session.parameters.Confirmation
import ru.solrudev.ackpine.session.parameters.ConfirmationAware
import ru.solrudev.ackpine.session.parameters.NotificationData

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
	public override val notificationData: NotificationData,

	/**
	 * Optional name of the session. It may be a name of the app being installed or a file name. Used in default
	 * notification content text.
	 */
	public val name: String,

	/**
	 * Indicate whether user action should be required when the session is committed. By default equals to `true`.
	 *
	 * Applying this option is best-effort. It takes effect only on API level >= [31][Build.VERSION_CODES.S] with
	 * [InstallerType.SESSION_BASED] installer type.
	 *
	 * @see [PackageInstaller.SessionParams.setRequireUserAction]
	 */
	public val requireUserAction: Boolean,

	/**
	 * Mode for an install session. Takes effect only when using [InstallerType.SESSION_BASED] installer.
	 *
	 * Default value is [InstallMode.Full].
	 */
	public val installMode: InstallMode,

	/**
	 * Details for requesting the pre-commit install approval.
	 *
	 * Applying this option is best-effort. It takes effect only on API level >=
	 * [34][Build.VERSION_CODES.UPSIDE_DOWN_CAKE] with [InstallerType.SESSION_BASED] installer type.
	 *
	 * Default value is [InstallPreapproval.NONE].
	 *
	 * @see [PackageInstaller.Session.requestUserPreapproval]
	 */
	public val preapproval: InstallPreapproval,

	/**
	 * Installation constraints.
	 *
	 * Applying this option is best-effort. It takes effect only on API level >=
	 * [34][Build.VERSION_CODES.UPSIDE_DOWN_CAKE] with [InstallerType.SESSION_BASED] installer type.
	 *
	 * Default value is [InstallConstraints.NONE].
	 *
	 * @see [PackageInstaller.InstallConstraints]
	 */
	public val constraints: InstallConstraints,

	/**
	 * Optionally indicate whether the package being installed needs the update ownership
	 * enforcement. Once the update ownership enforcement is enabled, the other installers
	 * will need the user action to update the package even if the installers have been
	 * granted the `INSTALL_PACKAGES` permission. Default to `false`.
	 *
	 * The update ownership enforcement can only be enabled on initial installation. Setting
	 * this to `true` on package update is a no-op.
	 *
	 * Applying this option is best-effort. It takes effect only on API level >=
	 * [34][Build.VERSION_CODES.UPSIDE_DOWN_CAKE] with [InstallerType.SESSION_BASED] installer type.
	 */
	public val requestUpdateOwnership: Boolean,

	/**
	 * Indicates the package source of the app being installed. This is informational and may be used as a signal
	 * by the system.
	 *
	 * Default value is [PackageSource.Unspecified].
	 */
	public val packageSource: PackageSource,

	/**
	 * [Plugins][AckpinePlugin] applied to the install session.
	 */
	public val plugins: AckpinePluginContainer
) : ConfirmationAware {

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false
		other as InstallParameters
		if (requireUserAction != other.requireUserAction) return false
		if (requestUpdateOwnership != other.requestUpdateOwnership) return false
		if (apks != other.apks) return false
		if (installerType != other.installerType) return false
		if (confirmation != other.confirmation) return false
		if (notificationData != other.notificationData) return false
		if (name != other.name) return false
		if (installMode != other.installMode) return false
		if (preapproval != other.preapproval) return false
		if (constraints != other.constraints) return false
		if (packageSource != other.packageSource) return false
		if (plugins != other.plugins) return false
		return true
	}

	override fun hashCode(): Int {
		var result = requireUserAction.hashCode()
		result = 31 * result + requestUpdateOwnership.hashCode()
		result = 31 * result + apks.hashCode()
		result = 31 * result + installerType.hashCode()
		result = 31 * result + confirmation.hashCode()
		result = 31 * result + notificationData.hashCode()
		result = 31 * result + name.hashCode()
		result = 31 * result + installMode.hashCode()
		result = 31 * result + preapproval.hashCode()
		result = 31 * result + constraints.hashCode()
		result = 31 * result + packageSource.hashCode()
		result = 31 * result + plugins.hashCode()
		return result
	}

	override fun toString(): String {
		return "InstallParameters(" +
				"apks=$apks, " +
				"installerType=$installerType, " +
				"confirmation=$confirmation, " +
				"notificationData=$notificationData, " +
				"name='$name', " +
				"requireUserAction=$requireUserAction, " +
				"installMode=$installMode, " +
				"preapproval=$preapproval, " +
				"constraints=$constraints, " +
				"requestUpdateOwnership=$requestUpdateOwnership, " +
				"packageSource=$packageSource, " +
				"plugins=$plugins" +
				")"
	}

	/**
	 * Builder for [InstallParameters].
	 */
	public class Builder : ConfirmationAware {

		@SuppressLint("NewApi")
		public constructor(baseApk: Uri) {
			_apks = RealMutableApkList(baseApk)
		}

		@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
		public constructor(apks: Iterable<Uri>) {
			_apks = RealMutableApkList(apks)
		}

		private val _apks: MutableApkList
		private val plugins = mutableMapOf<Class<out AckpinePlugin<*>>, AckpinePlugin.Parameters>()

		/**
		 * List of APKs [URIs][Uri] to install in one session.
		 */
		public val apks: ApkList
			get() = _apks

		/**
		 * Type of the package installer implementation.
		 *
		 * Default value is [InstallerType.DEFAULT].
		 *
		 * When getting/setting the value of this property, the following invariants are maintained:
		 * * When on API level < 21, [InstallerType.INTENT_BASED] is always returned/set regardless of the
		 * current/provided value;
		 * * When on API level >= 21 and [apks] contain more than one entry, [InstallerType.SESSION_BASED] is always
		 * returned/set regardless of the current/provided value.
		 */
		public var installerType: InstallerType = InstallerType.DEFAULT
			get() {
				field = applyInstallerTypeInvariants(field)
				return field
			}
			private set(value) {
				field = applyInstallerTypeInvariants(value)
			}

		/**
		 * A strategy for handling user's confirmation of installation or uninstallation.
		 *
		 * Default strategy is [Confirmation.DEFERRED].
		 */
		public override var confirmation: Confirmation = Confirmation.DEFERRED
			private set

		/**
		 * Data for a high-priority notification which launches confirmation activity.
		 *
		 * Default value is [NotificationData.DEFAULT].
		 *
		 * Ignored when [confirmation] is [Confirmation.IMMEDIATE].
		 */
		public override var notificationData: NotificationData = NotificationData.DEFAULT
			private set

		/**
		 * Optional name of the session. It may be a name of the app being installed or a file name. Used in default
		 * notification content text.
		 */
		public var name: String = ""
			private set

		/**
		 * Indicate whether user action should be required when the session is committed. By default equals to `true`.
		 *
		 * Applying this option is best-effort. It takes effect only on API level >= [31][Build.VERSION_CODES.S] with
		 * [InstallerType.SESSION_BASED] installer type.
		 *
		 * @see [PackageInstaller.SessionParams.setRequireUserAction]
		 */
		public var requireUserAction: Boolean = true
			private set

		/**
		 * Mode for an install session. Takes effect only when using [InstallerType.SESSION_BASED] installer.
		 *
		 * Default value is [InstallMode.Full].
		 */
		public var installMode: InstallMode = InstallMode.Full
			private set

		/**
		 * Details for requesting the pre-commit install approval.
		 *
		 * Applying this option is best-effort. It takes effect only on API level >=
		 * [34][Build.VERSION_CODES.UPSIDE_DOWN_CAKE] with [InstallerType.SESSION_BASED] installer type.
		 *
		 * Default value is [InstallPreapproval.NONE].
		 *
		 * @see [PackageInstaller.Session.requestUserPreapproval]
		 */
		public var preapproval: InstallPreapproval = InstallPreapproval.NONE
			private set

		/**
		 * Installation constraints.
		 *
		 * Applying this option is best-effort. It takes effect only on API level >=
		 * [34][Build.VERSION_CODES.UPSIDE_DOWN_CAKE] with [InstallerType.SESSION_BASED] installer type.
		 *
		 * Default value is [InstallConstraints.NONE].
		 *
		 * @see [PackageInstaller.InstallConstraints]
		 */
		public var constraints: InstallConstraints = InstallConstraints.NONE
			private set

		/**
		 * Optionally indicate whether the package being installed needs the update ownership
		 * enforcement. Once the update ownership enforcement is enabled, the other installers
		 * will need the user action to update the package even if the installers have been
		 * granted the `INSTALL_PACKAGES` permission. Default to `false`.
		 *
		 * The update ownership enforcement can only be enabled on initial installation. Set
		 * this to `true` on package update is a no-op.
		 *
		 * Applying this option is best-effort. It takes effect only on API level >=
		 * [34][Build.VERSION_CODES.UPSIDE_DOWN_CAKE] with [InstallerType.SESSION_BASED] installer type.
		 */
		public var requestUpdateOwnership: Boolean = false
			private set

		/**
		 * Indicates the package source of the app being installed. This is informational and may be used as a signal
		 * by the system.
		 *
		 * Default value is [PackageSource.Unspecified].
		 */
		public var packageSource: PackageSource = PackageSource.Unspecified
			private set

		/**
		 * Adds [apk] to [InstallParameters.apks].
		 */
		@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
		public fun addApk(apk: Uri): Builder = apply {
			_apks.add(apk)
		}

		/**
		 * Adds [apks] to [InstallParameters.apks].
		 */
		@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
		public fun addApks(apks: Iterable<Uri>): Builder = apply {
			_apks.addAll(apks)
		}

		/**
		 * Sets [InstallParameters.installerType], maintaining the following invariants:
		 * * When on API level < 21, [InstallerType.INTENT_BASED] is always set regardless of the provided value;
		 * * When on API level >= 21 and [apks] contains more than one entry, [InstallerType.SESSION_BASED] is always
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
		 * Sets [InstallParameters.name].
		 */
		public fun setName(name: String): Builder = apply {
			this.name = name
		}

		/**
		 * Sets [InstallParameters.requireUserAction].
		 *
		 * This is a **delicate** API. This option is unstable for use on different Android versions from different
		 * vendors. It's recommended to avoid using it on API level < 33 and on devices with modified OS package
		 * installer, most notably from Chinese vendors, unless your app is privileged for silent installs.
		 */
		@DelicateAckpineApi
		public fun setRequireUserAction(requireUserAction: Boolean): Builder = apply {
			this.requireUserAction = requireUserAction
		}

		/**
		 * Sets [InstallParameters.installMode].
		 */
		public fun setInstallMode(installMode: InstallMode): Builder = apply {
			this.installMode = installMode
		}

		/**
		 * Sets [InstallParameters.preapproval].
		 */
		public fun setPreapproval(preapproval: InstallPreapproval): Builder = apply {
			this.preapproval = preapproval
		}

		/**
		 * Sets [InstallParameters.constraints].
		 */
		public fun setConstraints(constraints: InstallConstraints): Builder = apply {
			this.constraints = constraints
		}

		/**
		 * Sets [InstallParameters.requestUpdateOwnership].
		 */
		public fun setRequestUpdateOwnership(requestUpdateOwnership: Boolean): Builder = apply {
			this.requestUpdateOwnership = requestUpdateOwnership
		}

		/**
		 * Sets [InstallParameters.packageSource].
		 */
		public fun setPackageSource(packageSource: PackageSource): Builder = apply {
			this.packageSource = packageSource
		}

		/**
		 * Applies a [plugin] to the session.
		 * @param plugin Java class of an applied plugin, implementing [AckpinePlugin].
		 * @param parameters parameters of the applied plugin for the session being configured.
		 */
		public fun <Params : AckpinePlugin.Parameters> usePlugin(
			plugin: Class<out AckpinePlugin<Params>>,
			parameters: Params
		): Builder = apply {
			plugins.put(plugin, parameters)
		}

		/**
		 * Applies a [plugin] to the session.
		 * @param plugin Java class of an applied plugin, implementing [AckpinePlugin].
		 */
		public fun usePlugin(plugin: Class<out AckpinePlugin<AckpinePlugin.Parameters.None>>): Builder = apply {
			plugins.put(plugin, AckpinePlugin.Parameters.None)
		}

		/**
		 * Constructs a new instance of [InstallParameters].
		 */
		@SuppressLint("NewApi")
		public fun build(): InstallParameters {
			val pluginContainer = AckpinePluginContainer.from(plugins)
			for (plugin in pluginContainer.getPluginInstances().keys) {
				plugin.apply(this)
			}
			return InstallParameters(
				ReadOnlyApkList(apks),
				installerType,
				confirmation,
				notificationData,
				name,
				requireUserAction,
				installMode,
				preapproval,
				constraints,
				requestUpdateOwnership,
				packageSource,
				pluginContainer
			)
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
		this.apks = apks.toMutableList()
		require(this.apks.isNotEmpty()) {
			"No APKs provided. It's required to have at least one base APK to create a session."
		}
		if (this.apks.size > 1) {
			checkSplitPackagesSupport()
		}
	}

	override val size: Int
		get() = apks.size

	private val apks: MutableList<Uri>

	override fun add(apk: Uri) {
		if (this.apks.isNotEmpty()) {
			checkSplitPackagesSupport()
		}
		this.apks.add(apk)
	}

	override fun addAll(apks: Iterable<Uri>) {
		val apksList = apks.toList()
		if (this.apks.isNotEmpty() || apksList.size > 1) {
			checkSplitPackagesSupport()
		}
		this.apks.addAll(apksList)
	}

	override fun toList() = apks.toList()

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is RealMutableApkList) return false
		return apks == other.apks
	}

	override fun hashCode() = apks.hashCode()
	override fun toString() = "ApkList($apks)"

	private fun checkSplitPackagesSupport() {
		if (!areSplitPackagesSupported()) {
			throw SplitPackagesNotSupportedException()
		}
	}
}

private class ReadOnlyApkList(private val apkList: ApkList) : ApkList by apkList {

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is ReadOnlyApkList) return false
		return apkList == other.apkList
	}

	override fun hashCode() = apkList.hashCode()
	override fun toString() = apkList.toString()
}