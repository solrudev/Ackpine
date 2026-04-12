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

package ru.solrudev.ackpine.privileged

import ru.solrudev.ackpine.capabilities.CapabilityStatus
import ru.solrudev.ackpine.capabilities.PluginCapability
import ru.solrudev.ackpine.installer.parameters.InstallerType
import ru.solrudev.ackpine.uninstaller.parameters.UninstallerType

/**
 * Shared install capabilities for privileged Ackpine plugins.
 *
 * Mirrors [PrivilegedInstallParameters]: each field indicates whether the corresponding parameter is supported for the
 * resolved configuration. Parameters are only effective when [PrivilegedPlugin] actually applies, but support here is
 * determined solely from the Android API level and the effective installer type.
 */
public abstract class PrivilegedInstallCapabilities protected constructor(

	/**
	 * Whether [PrivilegedInstallParameters.bypassLowTargetSdkBlock] is supported.
	 *
	 * [CapabilityStatus.SUPPORTED] on API level >= 34 with [InstallerType.SESSION_BASED].
	 */
	public val bypassLowTargetSdkBlock: CapabilityStatus,

	/**
	 * Whether [PrivilegedInstallParameters.allowTest] is supported.
	 *
	 * [CapabilityStatus.SUPPORTED] with [InstallerType.SESSION_BASED].
	 */
	public val allowTest: CapabilityStatus,

	/**
	 * Whether [PrivilegedInstallParameters.replaceExisting] is supported.
	 *
	 * [CapabilityStatus.SUPPORTED] with [InstallerType.SESSION_BASED].
	 */
	public val replaceExisting: CapabilityStatus,

	/**
	 * Whether [PrivilegedInstallParameters.requestDowngrade] is supported.
	 *
	 * [CapabilityStatus.SUPPORTED] with [InstallerType.SESSION_BASED].
	 */
	public val requestDowngrade: CapabilityStatus,

	/**
	 * Whether [PrivilegedInstallParameters.grantAllRequestedPermissions] is supported.
	 *
	 * [CapabilityStatus.SUPPORTED] on API level >= 23 with [InstallerType.SESSION_BASED].
	 */
	public val grantAllRequestedPermissions: CapabilityStatus,

	/**
	 * Whether [PrivilegedInstallParameters.allUsers] is supported.
	 *
	 * [CapabilityStatus.SUPPORTED] with [InstallerType.SESSION_BASED].
	 */
	public val allUsers: CapabilityStatus,

	/**
	 * Whether [PrivilegedInstallParameters.installerPackageName] is supported.
	 *
	 * [CapabilityStatus.SUPPORTED] on API level >= 28 with [InstallerType.SESSION_BASED].
	 */
	public val installerPackageName: CapabilityStatus
) : PluginCapability {

	/**
	 * Get the simple class name for toString().
	 */
	protected abstract fun getName(): String

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false
		other as PrivilegedInstallCapabilities
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
		return "${getName()}(" +
				"bypassLowTargetSdkBlock=$bypassLowTargetSdkBlock, " +
				"allowTest=$allowTest, " +
				"replaceExisting=$replaceExisting, " +
				"requestDowngrade=$requestDowngrade, " +
				"grantAllRequestedPermissions=$grantAllRequestedPermissions, " +
				"allUsers=$allUsers, " +
				"installerPackageName=$installerPackageName" +
				")"
	}
}

/**
 * Shared uninstall capabilities for privileged Ackpine plugins.
 *
 * Mirrors [PrivilegedUninstallParameters]: each field indicates whether the corresponding parameter is supported for
 * the resolved configuration. Parameters are only effective when [PrivilegedPlugin] or actually applies, but support
 * here is determined solely from the Android API level and the effective uninstaller type.
 */
public abstract class PrivilegedUninstallCapabilities protected constructor(

	/**
	 * Whether [PrivilegedUninstallParameters.keepData] is supported.
	 *
	 * [CapabilityStatus.SUPPORTED] with [UninstallerType.PACKAGE_INSTALLER_BASED].
	 */
	public val keepData: CapabilityStatus,

	/**
	 * Whether [PrivilegedUninstallParameters.allUsers] is supported.
	 *
	 * [CapabilityStatus.SUPPORTED] with [UninstallerType.PACKAGE_INSTALLER_BASED].
	 */
	public val allUsers: CapabilityStatus
) : PluginCapability {

	/**
	 * Get the simple class name for toString().
	 */
	protected abstract fun getName(): String

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false
		other as PrivilegedUninstallCapabilities
		if (keepData != other.keepData) return false
		if (allUsers != other.allUsers) return false
		return true
	}

	override fun hashCode(): Int {
		var result = keepData.hashCode()
		result = 31 * result + allUsers.hashCode()
		return result
	}

	override fun toString(): String = "${getName()}(" +
			"keepData=$keepData, " +
			"allUsers=$allUsers" +
			")"
}