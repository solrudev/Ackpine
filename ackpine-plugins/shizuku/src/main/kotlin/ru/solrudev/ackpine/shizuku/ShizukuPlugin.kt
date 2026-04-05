/*
 * Copyright (C) 2025 Ilya Fomichev
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

package ru.solrudev.ackpine.shizuku

import android.content.pm.PackageInstaller
import rikka.shizuku.Shizuku
import ru.solrudev.ackpine.DelicateAckpineApi
import ru.solrudev.ackpine.installer.parameters.InstallPreapproval
import ru.solrudev.ackpine.installer.parameters.InstallerType
import ru.solrudev.ackpine.installer.parameters.InstallerType.INTENT_BASED
import ru.solrudev.ackpine.plugability.AckpineInstallPlugin
import ru.solrudev.ackpine.plugability.AckpinePlugin
import ru.solrudev.ackpine.plugability.AckpineUninstallPlugin
import ru.solrudev.ackpine.uninstaller.parameters.UninstallerType
import ru.solrudev.ackpine.installer.parameters.InstallParameters as AckpineInstallParameters
import ru.solrudev.ackpine.uninstaller.parameters.UninstallParameters as AckpineUninstallParameters

/**
 * Ackpine plugin which enables installation and uninstallation through Shizuku when applied.
 *
 * Shizuku versions below 11 are not supported, and with these versions operations will fall back to normal system's
 * [PackageInstaller], or [INTENT_BASED] installer/uninstaller (if was set).
 *
 * **Note:** Shizuku permission and binder lifecycle are not managed by this Ackpine plugin. You must handle these in
 * your app to successfully use Shizuku.
 */
public class ShizukuPlugin private constructor() :
	AckpineInstallPlugin<ShizukuPlugin.InstallParameters>,
	AckpineUninstallPlugin<ShizukuPlugin.UninstallParameters> {

	override val id: String = PLUGIN_ID

	@OptIn(DelicateAckpineApi::class)
	override fun apply(builder: AckpineInstallParameters.Builder) {
		if (Shizuku.isPreV11()) {
			return
		}
		builder.apply {
			setInstallerType(InstallerType.SESSION_BASED)
			setRequireUserAction(false)
			setPreapproval(InstallPreapproval.NONE)
		}
	}

	override fun apply(builder: AckpineUninstallParameters.Builder) {
		if (Shizuku.isPreV11()) {
			return
		}
		builder.setUninstallerType(UninstallerType.PACKAGE_INSTALLER_BASED)
	}

	override fun equals(other: Any?): Boolean = this === other || other is ShizukuPlugin
	override fun hashCode(): Int = id.hashCode()
	override fun toString(): String = "ShizukuPlugin"

	/**
	 * Install parameters for [ShizukuPlugin].
	 */
	public open class InstallParameters internal constructor(

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
		 * Flag to indicate that an upgrade to a lower version of a package than currently installed has been requested.
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
		 * Builder for [ShizukuPlugin.InstallParameters].
		 */
		public open class Builder {

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
			 * Sets [ShizukuPlugin.InstallParameters.bypassLowTargetSdkBlock].
			 */
			public fun setBypassLowTargetSdkBlock(value: Boolean): Builder = apply {
				bypassLowTargetSdkBlock = value
			}

			/**
			 * Sets [ShizukuPlugin.InstallParameters.allowTest].
			 */
			public fun setAllowTest(value: Boolean): Builder = apply {
				allowTest = value
			}

			/**
			 * Sets [ShizukuPlugin.InstallParameters.replaceExisting].
			 */
			public fun setReplaceExisting(value: Boolean): Builder = apply {
				replaceExisting = value
			}

			/**
			 * Sets [ShizukuPlugin.InstallParameters.requestDowngrade].
			 */
			public fun setRequestDowngrade(value: Boolean): Builder = apply {
				requestDowngrade = value
			}

			/**
			 * Sets [ShizukuPlugin.InstallParameters.grantAllRequestedPermissions].
			 */
			public fun setGrantAllRequestedPermissions(value: Boolean): Builder = apply {
				grantAllRequestedPermissions = value
			}

			/**
			 * Sets [ShizukuPlugin.InstallParameters.allUsers].
			 */
			public fun setAllUsers(value: Boolean): Builder = apply {
				allUsers = value
			}

			/**
			 * Sets [ShizukuPlugin.InstallParameters.installerPackageName].
			 */
			public fun setInstallerPackageName(value: String): Builder = apply {
				installerPackageName = value
			}

			/**
			 * Constructs a new instance of [ShizukuPlugin.InstallParameters].
			 */
			public open fun build(): InstallParameters = InstallParameters(
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
			 * Default [ShizukuPlugin] install parameters.
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
	 * Install parameters for [ShizukuPlugin].
	 */
	@Deprecated(
		message = "Renamed to ShizukuPlugin.InstallParameters. " +
				"This will become an error in the next minor version.",
		replaceWith = ReplaceWith("ShizukuPlugin.InstallParameters")
	)
	@Suppress("DEPRECATION")
	public class Parameters private constructor(
		bypassLowTargetSdkBlock: Boolean,
		allowTest: Boolean,
		replaceExisting: Boolean,
		requestDowngrade: Boolean,
		grantAllRequestedPermissions: Boolean,
		allUsers: Boolean,
		installerPackageName: String
	) : InstallParameters(
		bypassLowTargetSdkBlock,
		allowTest,
		replaceExisting,
		requestDowngrade,
		grantAllRequestedPermissions,
		allUsers,
		installerPackageName
	) {

		/**
		 * Builder for [ShizukuPlugin.Parameters].
		 */
		@Deprecated(
			message = "Use ShizukuPlugin.InstallParameters.Builder instead. " +
					"This will become an error in the next minor version.",
			replaceWith = ReplaceWith("ShizukuPlugin.InstallParameters.Builder")
		)
		public class Builder : InstallParameters.Builder() {

			override fun build(): Parameters = Parameters(
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
			 * Default [ShizukuPlugin] parameters.
			 *
			 * All parameters are `false` by default.
			 */
			@Deprecated(
				message = "Use ShizukuPlugin.InstallParameters.DEFAULT instead. " +
						"This will become an error in the next minor version.",
				replaceWith = ReplaceWith("ShizukuPlugin.InstallParameters.DEFAULT")
			)
			@JvmField
			public val DEFAULT: Parameters = Parameters(
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
	 * Uninstall parameters for [ShizukuPlugin]. Take effect only on Android 8.1+.
	 */
	public open class UninstallParameters internal constructor(

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
		 * Builder for [ShizukuPlugin.UninstallParameters].
		 */
		public open class Builder {

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
			 * Sets [ShizukuPlugin.UninstallParameters.keepData].
			 */
			public fun setKeepData(value: Boolean): Builder = apply {
				keepData = value
			}

			/**
			 * Sets [ShizukuPlugin.UninstallParameters.allUsers].
			 */
			public fun setAllUsers(value: Boolean): Builder = apply {
				allUsers = value
			}

			/**
			 * Constructs a new instance of [ShizukuPlugin.UninstallParameters].
			 */
			public open fun build(): UninstallParameters = UninstallParameters(
				keepData,
				allUsers
			)
		}

		public companion object {

			/**
			 * Default [ShizukuPlugin] uninstall parameters.
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
		internal const val PLUGIN_ID = "ru.solrudev.ackpine.shizuku.ShizukuPlugin"
	}
}