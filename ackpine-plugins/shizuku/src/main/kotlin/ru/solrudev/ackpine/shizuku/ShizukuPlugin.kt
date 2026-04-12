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
import ru.solrudev.ackpine.capabilities.CapabilityStatus
import ru.solrudev.ackpine.capabilities.InstallCapabilityContext
import ru.solrudev.ackpine.capabilities.UninstallCapabilityContext
import ru.solrudev.ackpine.installer.parameters.InstallerType.INTENT_BASED
import ru.solrudev.ackpine.plugability.InstallPluginScope
import ru.solrudev.ackpine.plugability.UninstallPluginScope
import ru.solrudev.ackpine.privileged.PrivilegedInstallParameters
import ru.solrudev.ackpine.privileged.PrivilegedPlugin
import ru.solrudev.ackpine.privileged.PrivilegedUninstallParameters

/**
 * Ackpine plugin which enables installation and uninstallation through Shizuku when applied.
 *
 * Shizuku versions below 11 are not supported, and with these versions operations will fall back to normal system's
 * [PackageInstaller], or [INTENT_BASED] installer/uninstaller (if was set).
 *
 * **Note:** Shizuku permission and binder lifecycle are not managed by this Ackpine plugin. You must handle these in
 * your app to successfully use Shizuku.
 */
public class ShizukuPlugin private constructor() : PrivilegedPlugin<
		ShizukuPlugin.InstallParameters,
		ShizukuPlugin.UninstallParameters,
		ShizukuInstallCapabilities,
		ShizukuUninstallCapabilities
		>(PLUGIN_ID) {

	override fun apply(scope: InstallPluginScope) {
		if (Shizuku.isPreV11()) {
			return
		}
		super.apply(scope)
	}

	override fun apply(scope: UninstallPluginScope) {
		if (Shizuku.isPreV11()) {
			return
		}
		super.apply(scope)
	}

	@Suppress("RedundantOverride") // binary compatibility
	override fun getCapabilities(context: InstallCapabilityContext): ShizukuInstallCapabilities {
		return super.getCapabilities(context)
	}

	@Suppress("RedundantOverride") // binary compatibility
	override fun getCapabilities(context: UninstallCapabilityContext): ShizukuUninstallCapabilities {
		return super.getCapabilities(context)
	}

	override fun createInstallCapabilities(
		bypassLowTargetSdkBlock: CapabilityStatus,
		allowTest: CapabilityStatus,
		replaceExisting: CapabilityStatus,
		requestDowngrade: CapabilityStatus,
		grantAllRequestedPermissions: CapabilityStatus,
		allUsers: CapabilityStatus,
		installerPackageName: CapabilityStatus
	): ShizukuInstallCapabilities = ShizukuInstallCapabilities(
		bypassLowTargetSdkBlock = bypassLowTargetSdkBlock,
		allowTest = allowTest,
		replaceExisting = replaceExisting,
		requestDowngrade = requestDowngrade,
		grantAllRequestedPermissions = grantAllRequestedPermissions,
		allUsers = allUsers,
		installerPackageName = installerPackageName
	)

	override fun createUninstallCapabilities(
		keepData: CapabilityStatus,
		allUsers: CapabilityStatus
	): ShizukuUninstallCapabilities = ShizukuUninstallCapabilities(keepData, allUsers)

	/**
	 * Install parameters for [ShizukuPlugin].
	 */
	public open class InstallParameters internal constructor(
		bypassLowTargetSdkBlock: Boolean,
		allowTest: Boolean,
		replaceExisting: Boolean,
		requestDowngrade: Boolean,
		grantAllRequestedPermissions: Boolean,
		allUsers: Boolean,
		installerPackageName: String
	) : PrivilegedInstallParameters(
		bypassLowTargetSdkBlock,
		allowTest,
		replaceExisting,
		requestDowngrade,
		grantAllRequestedPermissions,
		allUsers,
		installerPackageName
	) {

		override fun getName(): String = "InstallParameters"

		/**
		 * Builder for [ShizukuPlugin.InstallParameters].
		 */
		@Suppress("RedundantOverride") // binary compatibility
		public open class Builder : PrivilegedInstallParameters.Builder<InstallParameters, Builder>() {

			override fun setBypassLowTargetSdkBlock(value: Boolean): Builder = super.setBypassLowTargetSdkBlock(value)
			override fun setAllowTest(value: Boolean): Builder = super.setAllowTest(value)
			override fun setReplaceExisting(value: Boolean): Builder = super.setReplaceExisting(value)
			override fun setRequestDowngrade(value: Boolean): Builder = super.setRequestDowngrade(value)
			override fun setGrantAllRequestedPermissions(value: Boolean): Builder =
				super.setGrantAllRequestedPermissions(value)

			override fun setAllUsers(value: Boolean): Builder = super.setAllUsers(value)
			override fun setInstallerPackageName(value: String): Builder = super.setInstallerPackageName(value)

			override fun build(): InstallParameters = InstallParameters(
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
			public val DEFAULT: InstallParameters = Builder().build()
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
			public val DEFAULT: Parameters = Builder().build()
		}
	}

	/**
	 * Uninstall parameters for [ShizukuPlugin]. Take effect only on Android 8.1+.
	 */
	public open class UninstallParameters internal constructor(
		keepData: Boolean,
		allUsers: Boolean
	) : PrivilegedUninstallParameters(keepData, allUsers) {

		override fun getName(): String = "UninstallParameters"

		/**
		 * Builder for [ShizukuPlugin.UninstallParameters].
		 */
		@Suppress("RedundantOverride") // binary compatibility
		public open class Builder : PrivilegedUninstallParameters.Builder<UninstallParameters, Builder>() {
			override fun setKeepData(value: Boolean): Builder = super.setKeepData(value)
			override fun setAllUsers(value: Boolean): Builder = super.setAllUsers(value)
			override fun build(): UninstallParameters = UninstallParameters(keepData, allUsers)
		}

		public companion object {

			/**
			 * Default [ShizukuPlugin] uninstall parameters.
			 *
			 * All parameters are `false` by default.
			 */
			@JvmField
			public val DEFAULT: UninstallParameters = Builder().build()
		}
	}

	internal companion object {
		@JvmSynthetic
		internal const val PLUGIN_ID = "ru.solrudev.ackpine.shizuku.ShizukuPlugin"
	}
}