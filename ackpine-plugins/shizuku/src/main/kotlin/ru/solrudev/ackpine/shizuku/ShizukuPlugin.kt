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

@file:Suppress("UnusedImport")

package ru.solrudev.ackpine.shizuku

import android.content.pm.PackageInstaller
import rikka.shizuku.Shizuku
import ru.solrudev.ackpine.installer.parameters.InstallerType.INTENT_BASED
import ru.solrudev.ackpine.plugability.InstallPluginScope
import ru.solrudev.ackpine.plugability.UninstallPluginScope
import ru.solrudev.ackpine.privileged.PrivilegedInstallCapabilities
import ru.solrudev.ackpine.privileged.PrivilegedInstallParameters
import ru.solrudev.ackpine.privileged.PrivilegedPlugin
import ru.solrudev.ackpine.privileged.PrivilegedUninstallCapabilities
import ru.solrudev.ackpine.privileged.PrivilegedUninstallParameters
import ru.solrudev.ackpine.privileged.TargetUser // KDoc

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

	override fun createInstallCapabilities(
		snapshot: PrivilegedInstallCapabilities.Snapshot
	): ShizukuInstallCapabilities = ShizukuInstallCapabilities(snapshot)

	override fun createUninstallCapabilities(
		snapshot: PrivilegedUninstallCapabilities.Snapshot
	): ShizukuUninstallCapabilities = ShizukuUninstallCapabilities(snapshot)

	/**
	 * Install parameters for [ShizukuPlugin].
	 */
	public class InstallParameters private constructor(snapshot: Snapshot) : PrivilegedInstallParameters(snapshot) {

		override fun getName(): String = "InstallParameters"

		/**
		 * Builder for [ShizukuPlugin.InstallParameters].
		 */
		public class Builder : PrivilegedInstallParameters.Builder<InstallParameters, Builder>() {
			override fun build(): InstallParameters = InstallParameters(buildSnapshot())
		}

		public companion object {

			/**
			 * Default [ShizukuPlugin] install parameters.
			 *
			 * All flags are `false` and [PrivilegedInstallParameters.targetUser] is [TargetUser.CURRENT] by default.
			 */
			@JvmField
			public val DEFAULT: InstallParameters = Builder().build()
		}
	}

	/**
	 * Uninstall parameters for [ShizukuPlugin].
	 */
	public class UninstallParameters private constructor(snapshot: Snapshot) : PrivilegedUninstallParameters(snapshot) {

		override fun getName(): String = "UninstallParameters"

		/**
		 * Builder for [ShizukuPlugin.UninstallParameters].
		 */
		public class Builder : PrivilegedUninstallParameters.Builder<UninstallParameters, Builder>() {
			override fun build(): UninstallParameters = UninstallParameters(buildSnapshot())
		}

		public companion object {

			/**
			 * Default [ShizukuPlugin] uninstall parameters.
			 *
			 * All flags are `false` and [PrivilegedUninstallParameters.targetUser] is [TargetUser.CURRENT] by default.
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