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

import android.os.Build
import ru.solrudev.ackpine.capabilities.CapabilityStatus
import ru.solrudev.ackpine.capabilities.InstallCapabilityContext
import ru.solrudev.ackpine.capabilities.InstallCapabilityProvider
import ru.solrudev.ackpine.capabilities.UninstallCapabilityContext
import ru.solrudev.ackpine.capabilities.UninstallCapabilityProvider
import ru.solrudev.ackpine.installer.parameters.InstallerType
import ru.solrudev.ackpine.plugability.AckpineInstallPlugin
import ru.solrudev.ackpine.plugability.AckpineUninstallPlugin
import ru.solrudev.ackpine.plugability.InstallPluginScope
import ru.solrudev.ackpine.plugability.UninstallPluginScope
import ru.solrudev.ackpine.uninstaller.parameters.UninstallerType

/**
 * Base implementation for privileged Ackpine plugins.
 */
public abstract class PrivilegedPlugin<
		InstallParams : PrivilegedInstallParameters,
		UninstallParams : PrivilegedUninstallParameters,
		InstallCapabilities : PrivilegedInstallCapabilities,
		UninstallCapabilities : PrivilegedUninstallCapabilities
		> protected constructor(final override val id: String) :
	AckpineInstallPlugin<InstallParams>,
	AckpineUninstallPlugin<UninstallParams>,
	InstallCapabilityProvider<InstallCapabilities>,
	UninstallCapabilityProvider<UninstallCapabilities> {

	override fun apply(scope: InstallPluginScope) {
		scope.installerType = InstallerType.SESSION_BASED
		scope.requireUserAction = false
		scope.disablePreapproval()
	}

	override fun apply(scope: UninstallPluginScope) {
		scope.uninstallerType = UninstallerType.PACKAGE_INSTALLER_BASED
	}

	override fun getCapabilities(context: InstallCapabilityContext): InstallCapabilities {
		val isSessionBased = context.installerType == InstallerType.SESSION_BASED
		val isSupported = if (isSessionBased) CapabilityStatus.SUPPORTED else CapabilityStatus.UNSUPPORTED
		fun isSupportedOnApi(api: Int) = if (isSessionBased && context.sdkInt >= api) {
			CapabilityStatus.SUPPORTED
		} else {
			CapabilityStatus.UNSUPPORTED
		}
		return createInstallCapabilities(
			bypassLowTargetSdkBlock = isSupportedOnApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE),
			allowTest = isSupported,
			replaceExisting = isSupported,
			requestDowngrade = isSupported,
			grantAllRequestedPermissions = isSupportedOnApi(Build.VERSION_CODES.M),
			allUsers = isSupported,
			installerPackageName = isSupportedOnApi(Build.VERSION_CODES.P)
		)
	}

	override fun getCapabilities(context: UninstallCapabilityContext): UninstallCapabilities {
		val isSupported = if (
			context.uninstallerType == UninstallerType.PACKAGE_INSTALLER_BASED &&
			context.sdkInt >= Build.VERSION_CODES.O_MR1
		) {
			CapabilityStatus.SUPPORTED
		} else {
			CapabilityStatus.UNSUPPORTED
		}
		return createUninstallCapabilities(keepData = isSupported, allUsers = isSupported)
	}

	/**
	 * Creates and returns the concrete [InstallCapabilities] instance from the pre-computed capability statuses.
	 */
	protected abstract fun createInstallCapabilities(
		bypassLowTargetSdkBlock: CapabilityStatus,
		allowTest: CapabilityStatus,
		replaceExisting: CapabilityStatus,
		requestDowngrade: CapabilityStatus,
		grantAllRequestedPermissions: CapabilityStatus,
		allUsers: CapabilityStatus,
		installerPackageName: CapabilityStatus
	): InstallCapabilities

	/**
	 * Creates and returns the concrete [UninstallCapabilities] instance from the pre-computed capability statuses.
	 */
	protected abstract fun createUninstallCapabilities(
		keepData: CapabilityStatus,
		allUsers: CapabilityStatus
	): UninstallCapabilities

	override fun equals(other: Any?): Boolean = this === other || other?.javaClass == javaClass
	override fun hashCode(): Int = id.hashCode()
	override fun toString(): String = javaClass.simpleName
}