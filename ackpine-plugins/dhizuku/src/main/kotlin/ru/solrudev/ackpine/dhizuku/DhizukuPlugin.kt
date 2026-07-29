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
		val requestDowngradeStatus = if (context.installerType == InstallerType.SESSION_BASED) {
			CapabilityStatus.UNRELIABLE
		} else {
			CapabilityStatus.UNSUPPORTED
		}
		return DhizukuInstallCapabilities(requestDowngrade = requestDowngradeStatus)
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
			systemApp = status
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
		public val requestDowngrade: Boolean
	) : AckpinePlugin.Parameters {

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false
			other as InstallParameters
			return requestDowngrade == other.requestDowngrade
		}

		override fun hashCode(): Int = requestDowngrade.hashCode()
		override fun toString(): String = "InstallParameters(requestDowngrade=$requestDowngrade)"

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
			 * Constructs a new instance of [InstallParameters].
			 */
			public fun build(): InstallParameters = InstallParameters(requestDowngrade)
		}

		public companion object {

			/**
			 * Default [DhizukuPlugin] install parameters with [requestDowngrade] disabled.
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
		public val systemApp: Boolean
	) : AckpinePlugin.Parameters {

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false
			other as UninstallParameters
			if (keepData != other.keepData) return false
			if (allUsers != other.allUsers) return false
			if (systemApp != other.systemApp) return false
			return true
		}

		override fun hashCode(): Int {
			var result = keepData.hashCode()
			result = 31 * result + allUsers.hashCode()
			result = 31 * result + systemApp.hashCode()
			return result
		}

		override fun toString(): String = "UninstallParameters(" +
				"keepData=$keepData, " +
				"allUsers=$allUsers, " +
				"systemApp=$systemApp" +
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
			 * Constructs a new instance of [UninstallParameters].
			 */
			public fun build(): UninstallParameters = UninstallParameters(
				keepData,
				allUsers,
				systemApp
			)
		}

		public companion object {

			/**
			 * Default [DhizukuPlugin] uninstall parameters with all flags disabled.
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