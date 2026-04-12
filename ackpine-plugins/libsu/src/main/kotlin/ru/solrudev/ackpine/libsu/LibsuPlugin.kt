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

package ru.solrudev.ackpine.libsu

import android.os.Build
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
 * Ackpine plugin which enables installation and uninstallation under root user via `libsu` when applied.
 *
 * **Note:** you must ensure that root access is available to successfully use this plugin. On first usage, root access
 * prompt from the root manager app (such as Magisk) will be shown to the user.
 */
public class LibsuPlugin :
	AckpineInstallPlugin<LibsuPlugin.InstallParameters>,
	AckpineUninstallPlugin<LibsuPlugin.UninstallParameters>,
	InstallCapabilityProvider<LibsuInstallCapabilities>,
	UninstallCapabilityProvider<LibsuUninstallCapabilities> {

	override val id: String = PLUGIN_ID

	override fun apply(scope: InstallPluginScope) {
		scope.installerType = InstallerType.SESSION_BASED
		scope.requireUserAction = false
		scope.disablePreapproval()
	}

	override fun apply(scope: UninstallPluginScope) {
		scope.uninstallerType = UninstallerType.PACKAGE_INSTALLER_BASED
	}

	override fun getCapabilities(context: InstallCapabilityContext): LibsuInstallCapabilities {
		val isSessionBased = context.installerType == InstallerType.SESSION_BASED
		val isSupported = if (isSessionBased) CapabilityStatus.SUPPORTED else CapabilityStatus.UNSUPPORTED
		fun isSupportedOnApi(api: Int) = if (isSessionBased && context.sdkInt >= api) {
			CapabilityStatus.SUPPORTED
		} else {
			CapabilityStatus.UNSUPPORTED
		}
		return LibsuInstallCapabilities(
			bypassLowTargetSdkBlock = isSupportedOnApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE),
			allowTest = isSupported,
			replaceExisting = isSupported,
			requestDowngrade = isSupported,
			grantAllRequestedPermissions = isSupportedOnApi(Build.VERSION_CODES.M),
			allUsers = isSupported,
			installerPackageName = isSupportedOnApi(Build.VERSION_CODES.P)
		)
	}

	override fun getCapabilities(context: UninstallCapabilityContext): LibsuUninstallCapabilities {
		val isSupported = if (
			context.uninstallerType == UninstallerType.PACKAGE_INSTALLER_BASED
			&& context.sdkInt >= 27
		) {
			CapabilityStatus.SUPPORTED
		} else {
			CapabilityStatus.UNSUPPORTED
		}
		return LibsuUninstallCapabilities(keepData = isSupported, allUsers = isSupported)
	}

	override fun equals(other: Any?): Boolean = this === other || other is LibsuPlugin
	override fun hashCode(): Int = id.hashCode()
	override fun toString(): String = "LibsuPlugin"

	/**
	 * Install parameters for [LibsuPlugin].
	 */
	public class InstallParameters private constructor(

		/**
		 * Flag to bypass the low target SDK version block for this install.
		 */
		public val bypassLowTargetSdkBlock: Boolean,

		/**
		 * Flag to indicate that you want to allow test packages (those that have set android:testOnly in their
		 * manifest) to be installed.
		 */
		public val allowTest: Boolean,

		/**
		 * Flag to indicate that you want to replace an already installed package, if one exists.
		 */
		public val replaceExisting: Boolean,

		/**
		 * Flag to indicate that an upgrade to a lower version of a package than currently installed has been
		 * requested.
		 */
		public val requestDowngrade: Boolean,

		/**
		 * Flag parameter for package install to indicate that all requested permissions should be granted to the
		 * package. If [allUsers] is set the runtime permissions will be granted to all users, otherwise only to the
		 * owner.
		 */
		public val grantAllRequestedPermissions: Boolean,

		/**
		 * Flag to indicate that this install should immediately be visible to all users.
		 */
		public val allUsers: Boolean,

		/**
		 * Installer package for the app. Empty by default, so the calling app package name will be used. Works only on
		 * Android 9+.
		 */
		public val installerPackageName: String
	) : AckpinePlugin.Parameters {

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (other !is InstallParameters) return false
			if (bypassLowTargetSdkBlock != other.bypassLowTargetSdkBlock) return false
			if (allowTest != other.allowTest) return false
			if (replaceExisting != other.replaceExisting) return false
			if (requestDowngrade != other.requestDowngrade) return false
			if (grantAllRequestedPermissions != other.grantAllRequestedPermissions) return false
			if (allUsers != other.allUsers) return false
			if (installerPackageName != other.installerPackageName) return false
			return true
		}

		override fun hashCode(): Int {
			var result = bypassLowTargetSdkBlock.hashCode()
			result = 31 * result + allowTest.hashCode()
			result = 31 * result + replaceExisting.hashCode()
			result = 31 * result + requestDowngrade.hashCode()
			result = 31 * result + grantAllRequestedPermissions.hashCode()
			result = 31 * result + allUsers.hashCode()
			result = 31 * result + installerPackageName.hashCode()
			return result
		}

		override fun toString(): String {
			return "InstallParameters(" +
					"bypassLowTargetSdkBlock=$bypassLowTargetSdkBlock, " +
					"allowTest=$allowTest, " +
					"replaceExisting=$replaceExisting, " +
					"requestDowngrade=$requestDowngrade, " +
					"grantAllRequestedPermissions=$grantAllRequestedPermissions, " +
					"allUsers=$allUsers, " +
					"installerPackageName=$installerPackageName" +
					")"
		}

		/**
		 * Builder for [LibsuPlugin.InstallParameters].
		 */
		public class Builder {

			/**
			 * Flag to bypass the low target SDK version block for this install.
			 */
			public var bypassLowTargetSdkBlock: Boolean = false
				private set

			/**
			 * Flag to indicate that you want to allow test packages (those that have set android:testOnly in their
			 * manifest) to be installed.
			 */
			public var allowTest: Boolean = false
				private set

			/**
			 * Flag to indicate that you want to replace an already installed package, if one exists.
			 */
			public var replaceExisting: Boolean = false
				private set

			/**
			 * Flag to indicate that an upgrade to a lower version of a package than currently installed has been
			 * requested.
			 */
			public var requestDowngrade: Boolean = false
				private set

			/**
			 * Flag parameter for package install to indicate that all requested permissions should be granted to the
			 * package. If [allUsers] is set the runtime permissions will be granted to all users, otherwise only to the
			 * owner.
			 */
			public var grantAllRequestedPermissions: Boolean = false
				private set

			/**
			 * Flag to indicate that this install should immediately be visible to all users.
			 */
			public var allUsers: Boolean = false
				private set

			/**
			 * Installer package for the app. Empty by default, so the calling app package name will be used. Takes
			 * effect only on Android 9+.
			 */
			public var installerPackageName: String = ""
				private set

			/**
			 * Sets [LibsuPlugin.InstallParameters.bypassLowTargetSdkBlock].
			 */
			public fun setBypassLowTargetSdkBlock(value: Boolean): Builder = apply {
				bypassLowTargetSdkBlock = value
			}

			/**
			 * Sets [LibsuPlugin.InstallParameters.allowTest].
			 */
			public fun setAllowTest(value: Boolean): Builder = apply {
				allowTest = value
			}

			/**
			 * Sets [LibsuPlugin.InstallParameters.replaceExisting].
			 */
			public fun setReplaceExisting(value: Boolean): Builder = apply {
				replaceExisting = value
			}

			/**
			 * Sets [LibsuPlugin.InstallParameters.requestDowngrade].
			 */
			public fun setRequestDowngrade(value: Boolean): Builder = apply {
				requestDowngrade = value
			}

			/**
			 * Sets [LibsuPlugin.InstallParameters.grantAllRequestedPermissions].
			 */
			public fun setGrantAllRequestedPermissions(value: Boolean): Builder = apply {
				grantAllRequestedPermissions = value
			}

			/**
			 * Sets [LibsuPlugin.InstallParameters.allUsers].
			 */
			public fun setAllUsers(value: Boolean): Builder = apply {
				allUsers = value
			}

			/**
			 * Sets [LibsuPlugin.InstallParameters.installerPackageName].
			 */
			public fun setInstallerPackageName(value: String): Builder = apply {
				installerPackageName = value
			}

			/**
			 * Constructs a new instance of [LibsuPlugin.InstallParameters].
			 */
			public fun build(): InstallParameters = InstallParameters(
				bypassLowTargetSdkBlock,
				allowTest,
				replaceExisting,
				requestDowngrade,
				grantAllRequestedPermissions,
				allUsers,
				installerPackageName
			)
		}

		public companion object {

			/**
			 * Default [LibsuPlugin] install parameters.
			 *
			 * All parameters are `false` by default.
			 */
			@JvmField
			public val DEFAULT: InstallParameters = InstallParameters(
				bypassLowTargetSdkBlock = false,
				allowTest = false,
				replaceExisting = false,
				requestDowngrade = false,
				grantAllRequestedPermissions = false,
				allUsers = false,
				installerPackageName = ""
			)
		}
	}

	/**
	 * Uninstall parameters for [LibsuPlugin]. Take effect only on Android 8.1+.
	 */
	public class UninstallParameters private constructor(

		/**
		 * Flag parameter to indicate that you don't want to delete the package's data directory.
		 */
		public val keepData: Boolean,

		/**
		 * Flag parameter to indicate that you want the package deleted for all users.
		 */
		public val allUsers: Boolean
	) : AckpinePlugin.Parameters {

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (other !is UninstallParameters) return false
			if (keepData != other.keepData) return false
			if (allUsers != other.allUsers) return false
			return true
		}

		override fun hashCode(): Int {
			var result = keepData.hashCode()
			result = 31 * result + allUsers.hashCode()
			return result
		}

		override fun toString(): String {
			return "UninstallParameters(" +
					"keepData=$keepData, " +
					"allUsers=$allUsers" +
					")"
		}

		/**
		 * Builder for [LibsuPlugin.UninstallParameters].
		 */
		public class Builder {

			/**
			 * Flag parameter to indicate that you don't want to delete the package's data directory.
			 */
			public var keepData: Boolean = false
				private set

			/**
			 * Flag parameter to indicate that you want the package deleted for all users.
			 */
			public var allUsers: Boolean = false
				private set

			/**
			 * Sets [LibsuPlugin.UninstallParameters.keepData].
			 */
			public fun setKeepData(value: Boolean): Builder = apply {
				keepData = value
			}

			/**
			 * Sets [LibsuPlugin.UninstallParameters.allUsers].
			 */
			public fun setAllUsers(value: Boolean): Builder = apply {
				allUsers = value
			}

			/**
			 * Constructs a new instance of [LibsuPlugin.UninstallParameters].
			 */
			public fun build(): UninstallParameters = UninstallParameters(
				keepData,
				allUsers
			)
		}

		public companion object {

			/**
			 * Default [LibsuPlugin] uninstall parameters.
			 *
			 * All parameters are `false` by default.
			 */
			@JvmField
			public val DEFAULT: UninstallParameters = UninstallParameters(
				keepData = false,
				allUsers = false
			)
		}
	}

	internal companion object {
		@JvmSynthetic
		internal const val PLUGIN_ID = "ru.solrudev.ackpine.libsu.LibsuPlugin"
	}
}