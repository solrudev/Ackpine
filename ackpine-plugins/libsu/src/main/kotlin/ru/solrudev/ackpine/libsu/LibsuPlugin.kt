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

import ru.solrudev.ackpine.capabilities.CapabilityStatus
import ru.solrudev.ackpine.privileged.PrivilegedInstallParameters
import ru.solrudev.ackpine.privileged.PrivilegedPlugin
import ru.solrudev.ackpine.privileged.PrivilegedUninstallParameters

/**
 * Ackpine plugin which enables installation and uninstallation under root user via `libsu` when applied.
 *
 * **Note:** you must ensure that root access is available to successfully use this plugin. On first usage, root access
 * prompt from the root manager app (such as Magisk) will be shown to the user if not already granted for your app.
 */
public class LibsuPlugin : PrivilegedPlugin<
		LibsuPlugin.InstallParameters,
		LibsuPlugin.UninstallParameters,
		LibsuInstallCapabilities,
		LibsuUninstallCapabilities
		>(PLUGIN_ID) {

	override fun createInstallCapabilities(
		bypassLowTargetSdkBlock: CapabilityStatus,
		allowTest: CapabilityStatus,
		replaceExisting: CapabilityStatus,
		requestDowngrade: CapabilityStatus,
		grantAllRequestedPermissions: CapabilityStatus,
		allUsers: CapabilityStatus,
		installerPackageName: CapabilityStatus
	): LibsuInstallCapabilities = LibsuInstallCapabilities(
		bypassLowTargetSdkBlock,
		allowTest,
		replaceExisting,
		requestDowngrade,
		grantAllRequestedPermissions,
		allUsers,
		installerPackageName
	)

	override fun createUninstallCapabilities(
		keepData: CapabilityStatus,
		allUsers: CapabilityStatus
	): LibsuUninstallCapabilities = LibsuUninstallCapabilities(keepData, allUsers)

	/**
	 * Install parameters for [LibsuPlugin].
	 */
	public class InstallParameters private constructor(
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
		 * Builder for [LibsuPlugin.InstallParameters].
		 */
		public class Builder : PrivilegedInstallParameters.Builder<InstallParameters, Builder>() {
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
			 * Default [LibsuPlugin] install parameters.
			 *
			 * All parameters are `false` by default.
			 */
			@JvmField
			public val DEFAULT: InstallParameters = Builder().build()
		}
	}

	/**
	 * Uninstall parameters for [LibsuPlugin]. Take effect only on Android 8.1+.
	 */
	public class UninstallParameters private constructor(
		keepData: Boolean,
		allUsers: Boolean
	) : PrivilegedUninstallParameters(keepData, allUsers) {

		override fun getName(): String = "UninstallParameters"

		/**
		 * Builder for [LibsuPlugin.UninstallParameters].
		 */
		public class Builder : PrivilegedUninstallParameters.Builder<UninstallParameters, Builder>() {
			override fun build(): UninstallParameters = UninstallParameters(keepData, allUsers)
		}

		public companion object {

			/**
			 * Default [LibsuPlugin] uninstall parameters.
			 *
			 * All parameters are `false` by default.
			 */
			@JvmField
			public val DEFAULT: UninstallParameters = Builder().build()
		}
	}

	internal companion object {
		@JvmSynthetic
		internal const val PLUGIN_ID = "ru.solrudev.ackpine.libsu.LibsuPlugin"
	}
}