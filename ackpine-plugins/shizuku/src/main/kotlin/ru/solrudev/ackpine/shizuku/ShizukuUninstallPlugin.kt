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
import ru.solrudev.ackpine.installer.parameters.InstallParameters
import ru.solrudev.ackpine.plugability.AckpinePlugin
import ru.solrudev.ackpine.uninstaller.parameters.UninstallParameters
import ru.solrudev.ackpine.uninstaller.parameters.UninstallerType
import ru.solrudev.ackpine.uninstaller.parameters.UninstallerType.INTENT_BASED

/**
 * Ackpine plugin which enables uninstallation through Shizuku when applied.
 *
 * This plugin's parameters take effect only on Android 8+.
 *
 * Shizuku versions below 11 are not supported, and with these versions uninstallations will fall back to normal
 * system's [PackageInstaller], or [INTENT_BASED] uninstaller (if was set).
 *
 * **Note:** Shizuku permission and binder lifecycle are not managed by this Ackpine plugin. You must handle these in
 * your app to successfully use Shizuku.
 */
public class ShizukuUninstallPlugin private constructor() : AckpinePlugin<ShizukuUninstallPlugin.Parameters> {

	override val id: String = PLUGIN_ID

	override fun apply(builder: InstallParameters.Builder) {
		error(
			"ShizukuUninstallPlugin must be applied to uninstall sessions only. " +
					"Use ShizukuPlugin for install sessions."
		)
	}

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
	 * Parameters for [ShizukuUninstallPlugin]. Take effect only on Android 8+.
	 */
	public class Parameters private constructor(

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
			if (javaClass != other?.javaClass) return false
			other as Parameters
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
			return "Parameters(" +
					"keepData=$keepData, " +
					"allUsers=$allUsers" +
					")"
		}

		/**
		 * Builder for [ShizukuUninstallPlugin.Parameters].
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
			 * Sets [ShizukuUninstallPlugin.Parameters.keepData].
			 */
			public fun setKeepData(value: Boolean): Builder = apply {
				keepData = value
			}

			/**
			 * Sets [ShizukuUninstallPlugin.Parameters.allUsers].
			 */
			public fun setAllUsers(value: Boolean): Builder = apply {
				allUsers = value
			}

			/**
			 * Constructs a new instance of [ShizukuUninstallPlugin.Parameters].
			 */
			public fun build(): Parameters = Parameters(
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