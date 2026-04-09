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

package ru.solrudev.ackpine.shizuku

import ru.solrudev.ackpine.capabilities.CapabilityStatus
import ru.solrudev.ackpine.capabilities.PluginCapability
import ru.solrudev.ackpine.uninstaller.parameters.UninstallerType

/**
 * Plugin-specific uninstall capabilities reported by [ShizukuPlugin] and [ShizukuUninstallPlugin].
 *
 * Mirrors [ShizukuPlugin.UninstallParameters]: each field indicates whether the corresponding parameter is
 * supported for the resolved configuration. Parameters are only effective when [ShizukuPlugin] or
 * [ShizukuUninstallPlugin] actually applies (i.e., when Shizuku version >= 11 is available at runtime), but support
 * here is determined solely from the Android API level and the effective uninstaller type.
 */
public class ShizukuUninstallCapabilities internal constructor(

	/**
	 * Whether [ShizukuPlugin.UninstallParameters.keepData] is supported.
	 *
	 * [CapabilityStatus.SUPPORTED] with [UninstallerType.PACKAGE_INSTALLER_BASED].
	 */
	public val keepData: CapabilityStatus,

	/**
	 * Whether [ShizukuPlugin.UninstallParameters.allUsers] is supported.
	 *
	 * [CapabilityStatus.SUPPORTED] with [UninstallerType.PACKAGE_INSTALLER_BASED].
	 */
	public val allUsers: CapabilityStatus
) : PluginCapability {

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is ShizukuUninstallCapabilities) return false
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
		return "ShizukuUninstallCapabilities(" +
				"keepData=$keepData, " +
				"allUsers=$allUsers" +
				")"
	}
}
