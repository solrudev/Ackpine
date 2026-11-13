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

import ru.solrudev.ackpine.capabilities.CapabilityStatus
import ru.solrudev.ackpine.capabilities.PluginCapability
import ru.solrudev.ackpine.installer.parameters.InstallerType
import ru.solrudev.ackpine.uninstaller.parameters.UninstallerType

/**
 * Plugin-specific install capabilities reported by [DhizukuPlugin].
 *
 * Mirrors [DhizukuPlugin.InstallParameters]. Parameters are only effective when [DhizukuPlugin] actually applies,
 * but support here is determined solely from the effective installer type.
 */
public class DhizukuInstallCapabilities internal constructor(

	/**
	 * Whether [DhizukuPlugin.InstallParameters.requestDowngrade] is supported.
	 *
	 * [CapabilityStatus.UNRELIABLE] with [InstallerType.SESSION_BASED].
	 */
	public val requestDowngrade: CapabilityStatus,

	/**
	 * Whether raw install flags are supported.
	 *
	 * [CapabilityStatus.UNRELIABLE] with [InstallerType.SESSION_BASED], because arbitrary flags may depend on Android
	 * version and device-owner permissions, otherwise [CapabilityStatus.UNSUPPORTED].
	 */
	public val installFlags: CapabilityStatus
) : PluginCapability {

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false
		other as DhizukuInstallCapabilities
		if (requestDowngrade != other.requestDowngrade) return false
		if (installFlags != other.installFlags) return false
		return true
	}

	override fun hashCode(): Int {
		var result = requestDowngrade.hashCode()
		result = 31 * result + installFlags.hashCode()
		return result
	}

	override fun toString(): String {
		return "DhizukuInstallCapabilities(" +
				"requestDowngrade=$requestDowngrade, " +
				"installFlags=$installFlags" +
				")"
	}
}

/**
 * Plugin-specific uninstall capabilities reported by [DhizukuPlugin].
 *
 * Mirrors [DhizukuPlugin.UninstallParameters]. Parameters are only effective when [DhizukuPlugin] actually applies,
 * but support here is determined solely from the effective uninstaller type.
 */
public class DhizukuUninstallCapabilities internal constructor(

	/**
	 * Whether [DhizukuPlugin.UninstallParameters.keepData] is supported.
	 *
	 * [CapabilityStatus.SUPPORTED] with [UninstallerType.PACKAGE_INSTALLER_BASED].
	 */
	public val keepData: CapabilityStatus,

	/**
	 * Whether [DhizukuPlugin.UninstallParameters.allUsers] is supported.
	 *
	 * [CapabilityStatus.SUPPORTED] with [UninstallerType.PACKAGE_INSTALLER_BASED].
	 */
	public val allUsers: CapabilityStatus,

	/**
	 * Whether [DhizukuPlugin.UninstallParameters.systemApp] is supported.
	 *
	 * [CapabilityStatus.SUPPORTED] with [UninstallerType.PACKAGE_INSTALLER_BASED].
	 */
	public val systemApp: CapabilityStatus,

	/**
	 * Whether [DhizukuPlugin.UninstallParameters.uninstallFlags] is supported.
	 *
	 * [CapabilityStatus.UNRELIABLE] with [UninstallerType.PACKAGE_INSTALLER_BASED], because arbitrary flags may depend
	 * on Android version and device-owner permissions, otherwise [CapabilityStatus.UNSUPPORTED].
	 */
	public val uninstallFlags: CapabilityStatus
) : PluginCapability {

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false
		other as DhizukuUninstallCapabilities
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

	override fun toString(): String = "DhizukuUninstallCapabilities(" +
			"keepData=$keepData, " +
			"allUsers=$allUsers, " +
			"systemApp=$systemApp, " +
			"uninstallFlags=$uninstallFlags" +
			")"
}