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

@file:Suppress("UnusedImport")

package ru.solrudev.ackpine.libsu

import ru.solrudev.ackpine.privileged.PrivilegedInstallCapabilities
import ru.solrudev.ackpine.privileged.PrivilegedInstallParameters
import ru.solrudev.ackpine.privileged.PrivilegedPlugin
import ru.solrudev.ackpine.privileged.PrivilegedUninstallCapabilities
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
		snapshot: PrivilegedInstallCapabilities.Snapshot
	): LibsuInstallCapabilities = LibsuInstallCapabilities(snapshot)

	override fun createUninstallCapabilities(
		snapshot: PrivilegedUninstallCapabilities.Snapshot
	): LibsuUninstallCapabilities = LibsuUninstallCapabilities(snapshot)

	/**
	 * Install parameters for [LibsuPlugin].
	 */
	public class InstallParameters private constructor(snapshot: Snapshot) : PrivilegedInstallParameters(snapshot) {

		override fun getName(): String = "InstallParameters"

		/**
		 * Builder for [LibsuPlugin.InstallParameters].
		 */
		public class Builder : PrivilegedInstallParameters.Builder<InstallParameters, Builder>() {
			override fun build(): InstallParameters = InstallParameters(buildSnapshot())
		}

		public companion object {

			/**
			 * Default [LibsuPlugin] install parameters.
			 *
			 * All flags are `false` and [PrivilegedInstallParameters.targetUser] is [TargetUser.CURRENT] by default.
			 */
			@JvmField
			public val DEFAULT: InstallParameters = Builder().build()
		}
	}

	/**
	 * Uninstall parameters for [LibsuPlugin]. Uninstall flags take effect only on Android 8.1+.
	 */
	public class UninstallParameters private constructor(snapshot: Snapshot) : PrivilegedUninstallParameters(snapshot) {

		override fun getName(): String = "UninstallParameters"

		/**
		 * Builder for [LibsuPlugin.UninstallParameters].
		 */
		public class Builder : PrivilegedUninstallParameters.Builder<UninstallParameters, Builder>() {
			override fun build(): UninstallParameters = UninstallParameters(buildSnapshot())
		}

		public companion object {

			/**
			 * Default [LibsuPlugin] uninstall parameters.
			 *
			 * All flags are `false` and [PrivilegedUninstallParameters.targetUser] is [TargetUser.CURRENT] by default.
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