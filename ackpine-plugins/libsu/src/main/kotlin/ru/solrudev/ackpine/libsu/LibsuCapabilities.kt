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
import ru.solrudev.ackpine.privileged.PrivilegedInstallCapabilities
import ru.solrudev.ackpine.privileged.PrivilegedUninstallCapabilities

/**
 * Plugin-specific install capabilities reported by [LibsuPlugin].
 *
 * Mirrors [LibsuPlugin.InstallParameters]: each field indicates whether the corresponding parameter is supported
 * for the resolved configuration. Parameters are only effective when [LibsuPlugin] actually applies, but support
 * here is determined solely from the Android API level and the effective installer type.
 */
public class LibsuInstallCapabilities internal constructor(
	bypassLowTargetSdkBlock: CapabilityStatus,
	allowTest: CapabilityStatus,
	replaceExisting: CapabilityStatus,
	requestDowngrade: CapabilityStatus,
	grantAllRequestedPermissions: CapabilityStatus,
	allUsers: CapabilityStatus,
	installerPackageName: CapabilityStatus
) : PrivilegedInstallCapabilities(
	bypassLowTargetSdkBlock,
	allowTest,
	replaceExisting,
	requestDowngrade,
	grantAllRequestedPermissions,
	allUsers,
	installerPackageName
) {
	override fun getName(): String = "LibsuInstallCapabilities"
}

/**
 * Plugin-specific uninstall capabilities reported by [LibsuPlugin].
 *
 * Mirrors [LibsuPlugin.UninstallParameters]: each field indicates whether the corresponding parameter is
 * supported for the resolved configuration. Parameters are only effective when [LibsuPlugin] actually applies, but
 * support here is determined solely from the Android API level and the effective uninstaller type.
 */
public class LibsuUninstallCapabilities internal constructor(
	keepData: CapabilityStatus,
	allUsers: CapabilityStatus
) : PrivilegedUninstallCapabilities(keepData, allUsers) {
	override fun getName(): String = "LibsuUninstallCapabilities"
}