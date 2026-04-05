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
import ru.solrudev.ackpine.plugability.AckpineUninstallPlugin
import ru.solrudev.ackpine.uninstaller.parameters.UninstallParameters
import ru.solrudev.ackpine.uninstaller.parameters.UninstallerType
import ru.solrudev.ackpine.uninstaller.parameters.UninstallerType.INTENT_BASED

/**
 * Ackpine plugin which enables uninstallation through Shizuku when applied.
 *
 * This plugin's parameters take effect only on Android 8.1+.
 *
 * Shizuku versions below 11 are not supported, and with these versions uninstallations will fall back to normal
 * system's [PackageInstaller], or [INTENT_BASED] uninstaller (if was set).
 *
 * **Note:** Shizuku permission and binder lifecycle are not managed by this Ackpine plugin. You must handle these in
 * your app to successfully use Shizuku.
 */
@Deprecated(
	message = "Use ShizukuPlugin for both install and uninstall sessions. " +
			"This will become an error in the next minor version.",
	replaceWith = ReplaceWith("ShizukuPlugin", "ru.solrudev.ackpine.shizuku.ShizukuPlugin")
)
@Suppress("DEPRECATION")
public class ShizukuUninstallPlugin private constructor() : AckpineUninstallPlugin<ShizukuUninstallPlugin.Parameters> {

	override val id: String = PLUGIN_ID

	override fun apply(builder: UninstallParameters.Builder) {
		if (Shizuku.isPreV11()) {
			return
		}
		builder.setUninstallerType(UninstallerType.PACKAGE_INSTALLER_BASED)
	}

	override fun equals(other: Any?): Boolean = this === other || other is ShizukuUninstallPlugin
	override fun hashCode(): Int = id.hashCode()
	override fun toString(): String = "ShizukuUninstallPlugin"

	/**
	 * Parameters for [ShizukuUninstallPlugin]. Take effect only on Android 8.1+.
	 */
	@Deprecated(
		message = "Use ShizukuPlugin.UninstallParameters instead. This will become an error in the next minor version.",
		replaceWith = ReplaceWith(
			"ShizukuPlugin.UninstallParameters",
			"ru.solrudev.ackpine.shizuku.ShizukuPlugin"
		)
	)
	public class Parameters private constructor(
		keepData: Boolean,
		allUsers: Boolean
	) : ShizukuPlugin.UninstallParameters(keepData, allUsers) {

		/**
		 * Builder for [ShizukuUninstallPlugin.Parameters].
		 */
		@Deprecated(
			message = "Use ShizukuPlugin.UninstallParameters.Builder instead. " +
					"This will become an error in the next minor version.",
			replaceWith = ReplaceWith(
				"ShizukuPlugin.UninstallParameters.Builder",
				"ru.solrudev.ackpine.shizuku.ShizukuPlugin"
			)
		)
		public class Builder : ShizukuPlugin.UninstallParameters.Builder() {

			/**
			 * Constructs a new instance of [ShizukuUninstallPlugin.Parameters].
			 */
			override fun build(): Parameters = Parameters(
				keepData,
				allUsers
			)
		}

		public companion object {

			/**
			 * Default [ShizukuUninstallPlugin] parameters.
			 *
			 * All parameters are `false` by default.
			 */
			@Deprecated(
				message = "Use ShizukuPlugin.UninstallParameters.DEFAULT instead. " +
						"This will become an error in the next minor version.",
				replaceWith = ReplaceWith(
					"ShizukuPlugin.UninstallParameters.DEFAULT",
					"ru.solrudev.ackpine.shizuku.ShizukuPlugin"
				)
			)
			@JvmField
			public val DEFAULT: Parameters = Parameters(
				keepData = false,
				allUsers = false
			)
		}
	}

	internal companion object {
		@JvmSynthetic
		internal const val PLUGIN_ID = "ru.solrudev.ackpine.shizuku.ShizukuUninstallPlugin"
	}
}