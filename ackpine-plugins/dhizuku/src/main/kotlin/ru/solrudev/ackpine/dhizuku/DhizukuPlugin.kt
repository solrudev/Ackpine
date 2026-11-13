/*
 * Copyright (C) 2026 Ilya Fomichev
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

package ru.solrudev.ackpine.dhizuku

import ru.solrudev.ackpine.DelicateAckpineApi
import ru.solrudev.ackpine.capabilities.CapabilityStatus
import ru.solrudev.ackpine.capabilities.InstallCapabilityContext
import ru.solrudev.ackpine.capabilities.InstallCapabilityProvider
import ru.solrudev.ackpine.capabilities.UninstallCapabilityContext
import ru.solrudev.ackpine.capabilities.UninstallCapabilityProvider
import ru.solrudev.ackpine.installer.parameters.InstallerType
import ru.solrudev.ackpine.plugability.AckpineInstallPlugin
import ru.solrudev.ackpine.plugability.AckpinePlugin
import ru.solrudev.ackpine.plugability.AckpineUninstallPlugin
import ru.solrudev.ackpine.plugability.InstallPluginScope
import ru.solrudev.ackpine.plugability.UninstallPluginScope
import ru.solrudev.ackpine.uninstaller.parameters.UninstallerType

/**
 * Ackpine plugin which enables installation and uninstallation through Dhizuku when applied.
 *
 * Dhizuku performs package installer operations as the active device owner application. Accordingly,
 * [InstallParameters] and [UninstallParameters] expose only options supported for that identity.
 * [Arbitrary install][DhizukuPlugin.InstallParameters.installFlags] and
 * [uninstall flags][DhizukuPlugin.UninstallParameters.uninstallFlags] can be supplied, but there's no guarantee that
 * they will affect the outcome or that the session will succeed when they are applied.
 *
 * **Note:** Dhizuku activation and permission are not managed by this Ackpine plugin. You must handle them in your app
 * to successfully use Dhizuku.
 */
public class DhizukuPlugin :
	AckpineInstallPlugin<DhizukuPlugin.InstallParameters>,
	AckpineUninstallPlugin<DhizukuPlugin.UninstallParameters>,
	InstallCapabilityProvider<DhizukuInstallCapabilities>,
	UninstallCapabilityProvider<DhizukuUninstallCapabilities> {

	override val id: String = PLUGIN_ID

	override fun apply(scope: InstallPluginScope) {
		scope.installerType = InstallerType.SESSION_BASED
		scope.requireUserAction = false
		scope.disablePreapproval()
	}

	override fun apply(scope: UninstallPluginScope) {
		scope.uninstallerType = UninstallerType.PACKAGE_INSTALLER_BASED
	}

	override fun getCapabilities(context: InstallCapabilityContext): DhizukuInstallCapabilities {
		val capabilityStatus = if (context.installerType == InstallerType.SESSION_BASED) {
			CapabilityStatus.UNRELIABLE
		} else {
			CapabilityStatus.UNSUPPORTED
		}
		return DhizukuInstallCapabilities(
			requestDowngrade = capabilityStatus,
			installFlags = capabilityStatus
		)
	}

	override fun getCapabilities(context: UninstallCapabilityContext): DhizukuUninstallCapabilities {
		val status = if (context.uninstallerType == UninstallerType.PACKAGE_INSTALLER_BASED) {
			CapabilityStatus.SUPPORTED
		} else {
			CapabilityStatus.UNSUPPORTED
		}
		return DhizukuUninstallCapabilities(
			keepData = status,
			allUsers = status,
			systemApp = status,
			uninstallFlags = if (status == CapabilityStatus.SUPPORTED) {
				CapabilityStatus.UNRELIABLE
			} else {
				CapabilityStatus.UNSUPPORTED
			}
		)
	}

	override fun equals(other: Any?): Boolean = this === other || other?.javaClass == javaClass
	override fun hashCode(): Int = id.hashCode()
	override fun toString(): String = javaClass.simpleName

	/**
	 * Install parameters for [DhizukuPlugin].
	 */
	public class InstallParameters private constructor(

		/**
		 * Whether an upgrade to a lower version of a package than currently installed is requested.
		 *
		 * Ackpine sets downgrade-request flag which is honored for debuggable installed apps or debuggable OS builds,
		 * but does not add the unrestricted downgrade permission flag used by root, shell, or system identities.
		 * The device owner must still be authorized by Android to perform the downgrade, so setting this option does
		 * not guarantee that a downgrade will succeed.
		 */
		public val requestDowngrade: Boolean,

		/**
		 * Raw Android install flags mask, `0` by default. All `Int` bit patterns are accepted.
		 *
		 * The mask is ORed with platform flags _before_ applying Dhizuku's boolean options. These options take
		 * precedence for their corresponding bits, including clearing them when `false`. Flags without corresponding
		 * Dhizuku boolean options remain unchanged.
		 * The boolean values remain independent of this mask. Setting the mask replaces the previous supplied mask;
		 * unrelated platform flags are preserved.
		 *
		 * Setting this value is a **delicate** API: flags availability and behavior may depend on the Android version
		 * and device-owner permissions. Supplying flags grants no additional privileges.
		 */
		public val installFlags: Int
	) : AckpinePlugin.Parameters {

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false
			other as InstallParameters
			if (requestDowngrade != other.requestDowngrade) return false
			if (installFlags != other.installFlags) return false
			return true
		}

		override fun hashCode(): Int {
			var result = requestDowngrade.hashCode()
			result = 31 * result + installFlags
			return result
		}

		override fun toString(): String {
			return "InstallParameters(" +
					"requestDowngrade=$requestDowngrade, " +
					"installFlags=$installFlags" +
					")"
		}

		/**
		 * Builder for [DhizukuPlugin.InstallParameters].
		 */
		public class Builder {

			/**
			 * Whether an upgrade to a lower version of a package than currently installed is requested.
			 *
			 * Ackpine sets downgrade-request flag which is honored for debuggable installed apps or debuggable OS
			 * builds, but does not add the unrestricted downgrade permission flag used by root, shell, or system
			 * identities.
			 * The device owner must still be authorized by Android to perform the downgrade, so setting this option
			 * does not guarantee that a downgrade will succeed.
			 */
			public var requestDowngrade: Boolean = false
				private set

			/**
			 * Raw Android install flags mask, `0` by default. All `Int` bit patterns are accepted.
			 *
			 * The mask is ORed with platform flags _before_ applying Dhizuku's boolean options. These options take
			 * precedence for their corresponding bits, including clearing them when `false`. Flags without
			 * corresponding Dhizuku boolean options remain unchanged.
			 * The boolean values remain independent of this mask. Setting the mask replaces the previous supplied mask;
			 * unrelated platform flags are preserved.
			 *
			 * Setting this value is a **delicate** API: flags availability and behavior may depend on the Android
			 * version and device-owner permissions. Supplying flags grants no additional privileges.
			 */
			public var installFlags: Int = 0
				private set

			/**
			 * Sets [InstallParameters.requestDowngrade].
			 *
			 * This is a **delicate** API. The outcome of using this flag depends on the app being installed, on a
			 * specific OS build, and on the permissions that the used device owner identity has.
			 */
			@DelicateAckpineApi
			public fun setRequestDowngrade(value: Boolean): Builder = apply {
				requestDowngrade = value
			}

			/**
			 * Sets [InstallParameters.installFlags].
			 *
			 * This is a **delicate** API.
			 */
			@DelicateAckpineApi
			public fun setInstallFlags(value: Int): Builder = apply {
				installFlags = value
			}

			/**
			 * Constructs a new instance of [InstallParameters].
			 */
			public fun build(): InstallParameters = InstallParameters(requestDowngrade, installFlags)
		}

		public companion object {

			/**
			 * Default [DhizukuPlugin] install parameters with [requestDowngrade] disabled and [installFlags] equal to
			 * `0`.
			 */
			@JvmField
			public val DEFAULT: InstallParameters = Builder().build()
		}
	}

	/**
	 * Uninstall parameters for [DhizukuPlugin].
	 *
	 * Dhizuku uninstall operations always target the current Android user unless [allUsers] is enabled.
	 */
	public class UninstallParameters private constructor(

		/**
		 * Whether to retain the package's data directory.
		 */
		public val keepData: Boolean,

		/**
		 * Whether to delete the package for all users.
		 */
		public val allUsers: Boolean,

		/**
		 * Whether a system app should be marked as uninstalled for the current user.
		 *
		 * This does not remove the app from the system partition. For an updated system app, it prevents the update
		 * from being rolled back globally when uninstalling it for the current user.
		 */
		public val systemApp: Boolean,

		/**
		 * Raw Android uninstall flags mask, `0` by default. All `Int` bit patterns are accepted.
		 *
		 * The mask is applied _before_ Dhizuku's boolean options. These options take precedence for their corresponding
		 * bits, including clearing them when `false`. Flags without corresponding Dhizuku boolean options remain
		 * unchanged. The boolean values remain independent of this mask.
		 * Setting the mask replaces the previous supplied mask.
		 *
		 * Setting this value is a **delicate** API: flags availability and behavior may depend on the Android version
		 * and device-owner permissions. Supplying flags grants no additional privileges.
		 */
		public val uninstallFlags: Int
	) : AckpinePlugin.Parameters {

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false
			other as UninstallParameters
			if (keepData != other.keepData) return false
			if (allUsers != other.allUsers) return false
			if (systemApp != other.systemApp) return false
			if (uninstallFlags != other.uninstallFlags) return false
			return true
		}

		override fun hashCode(): Int {
			var result = keepData.hashCode()
			result = 31 * result + allUsers.hashCode()
			result = 31 * result + systemApp.hashCode()
			result = 31 * result + uninstallFlags.hashCode()
			return result
		}

		override fun toString(): String = "UninstallParameters(" +
				"keepData=$keepData, " +
				"allUsers=$allUsers, " +
				"systemApp=$systemApp, " +
				"uninstallFlags=$uninstallFlags" +
				")"

		/**
		 * Builder for [DhizukuPlugin.UninstallParameters].
		 */
		public class Builder {

			/**
			 * Whether to retain the package's data directory.
			 */
			public var keepData: Boolean = false
				private set

			/**
			 * Whether to delete the package for all users.
			 */
			public var allUsers: Boolean = false
				private set

			/**
			 * Whether a system app should be marked as uninstalled for the current user.
			 */
			public var systemApp: Boolean = false
				private set

			/**
			 * Raw Android uninstall flags mask, `0` by default. All `Int` bit patterns are accepted.
			 *
			 * The mask is applied _before_ Dhizuku's boolean options. These options take precedence for their corresponding
			 * bits, including clearing them when `false`. Flags without corresponding Dhizuku boolean options remain
			 * unchanged. The boolean values remain independent of this mask.
			 * Setting the mask replaces the previous supplied mask.
			 *
			 * Setting this value is a **delicate** API: flags availability and behavior may depend on the Android
			 * version and device-owner permissions. Supplying flags grants no additional privileges.
			 */
			public var uninstallFlags: Int = 0
				private set

			/**
			 * Sets [UninstallParameters.keepData].
			 */
			public fun setKeepData(value: Boolean): Builder = apply {
				keepData = value
			}

			/**
			 * Sets [UninstallParameters.allUsers].
			 */
			public fun setAllUsers(value: Boolean): Builder = apply {
				allUsers = value
			}

			/**
			 * Sets [UninstallParameters.systemApp].
			 */
			public fun setSystemApp(value: Boolean): Builder = apply {
				systemApp = value
			}

			/**
			 * Sets [UninstallParameters.uninstallFlags].
			 *
			 * This is a **delicate** API.
			 */
			@DelicateAckpineApi
			public fun setUninstallFlags(value: Int): Builder = apply {
				uninstallFlags = value
			}

			/**
			 * Constructs a new instance of [UninstallParameters].
			 */
			public fun build(): UninstallParameters = UninstallParameters(
				keepData,
				allUsers,
				systemApp,
				uninstallFlags
			)
		}

		public companion object {

			/**
			 * Default [DhizukuPlugin] uninstall parameters with all boolean flags disabled and [uninstallFlags] equal
			 * to `0`.
			 */
			@JvmField
			public val DEFAULT: UninstallParameters = Builder().build()
		}
	}

	internal companion object {
		@JvmSynthetic
		internal const val PLUGIN_ID = "ru.solrudev.ackpine.dhizuku.DhizukuPlugin"
	}
}