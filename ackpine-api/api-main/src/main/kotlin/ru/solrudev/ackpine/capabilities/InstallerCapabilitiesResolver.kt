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

package ru.solrudev.ackpine.capabilities

import android.os.Build
import ru.solrudev.ackpine.SdkIntWrapper
import ru.solrudev.ackpine.installer.parameters.InstallerType
import ru.solrudev.ackpine.isPackageInstallerApiAvailable
import ru.solrudev.ackpine.plugability.AckpineInstallPlugin
import ru.solrudev.ackpine.plugability.AckpinePluginCache
import ru.solrudev.ackpine.plugability.InstallPluginScope

@JvmSynthetic
internal fun resolveInstallerCapabilities(
	requestedType: InstallerType,
	pluginClasses: List<Class<out AckpineInstallPlugin<*>>>
): InstallerCapabilities {
	val sdkInt = SdkIntWrapper.get()
	val scope = InstallPluginScope.create(requestedType)
	pluginClasses.forEach(scope::registerCapabilityPlugin)
	scope.resolvePlugins(::normalizeInstallerType)
	val finalType = scope.installerType
	val capabilities = resolveBaseInstallerCapabilities(finalType, sdkInt)
	val context = InstallCapabilityContext(sdkInt, finalType)
	return InstallerCapabilities(
		installerType = finalType,
		skipUserAction = isAvailable(capabilities::skipUserAction, scope.isUserActionForced),
		preapproval = isAvailable(capabilities::preapproval, scope.isPreapprovalDisabled),
		constraints = isAvailable(capabilities::constraints, scope.isConstraintsDisabled),
		requestUpdateOwnership = isAvailable(capabilities::requestUpdateOwnership, scope.isUpdateOwnershipDisabled),
		packageSource = capabilities.packageSource,
		dontKillApp = capabilities.dontKillApp,
		pluginCapabilities = collectInstallPluginCapabilities(scope, context)
	)
}

private fun resolveBaseInstallerCapabilities(
	installerType: InstallerType,
	sdkInt: Int
): BaseInstallerCapabilities {
	if (installerType == InstallerType.INTENT_BASED) {
		return BaseInstallerCapabilities(
			skipUserAction = CapabilityStatus.UNSUPPORTED,
			preapproval = CapabilityStatus.UNSUPPORTED,
			constraints = CapabilityStatus.UNSUPPORTED,
			requestUpdateOwnership = CapabilityStatus.UNSUPPORTED,
			packageSource = CapabilityStatus.UNSUPPORTED,
			dontKillApp = CapabilityStatus.UNSUPPORTED
		)
	}
	val api34Support = if (sdkInt >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
		CapabilityStatus.SUPPORTED
	} else {
		CapabilityStatus.UNSUPPORTED
	}
	return BaseInstallerCapabilities(
		skipUserAction = if (sdkInt >= Build.VERSION_CODES.S) {
			CapabilityStatus.UNRELIABLE
		} else {
			CapabilityStatus.UNSUPPORTED
		},
		preapproval = api34Support,
		constraints = api34Support,
		requestUpdateOwnership = api34Support,
		packageSource = if (sdkInt >= Build.VERSION_CODES.TIRAMISU) {
			CapabilityStatus.SUPPORTED
		} else {
			CapabilityStatus.UNSUPPORTED
		},
		dontKillApp = api34Support
	)
}

private inline fun isAvailable(
	capabilitySelector: () -> CapabilityStatus,
	isUnsupported: Boolean
): CapabilityStatus {
	val capability = capabilitySelector()
	if (capability.isAvailable && isUnsupported) {
		return CapabilityStatus.UNSUPPORTED
	}
	return capability
}

private fun collectInstallPluginCapabilities(
	scope: InstallPluginScope,
	context: InstallCapabilityContext
): Map<Class<out InstallCapabilityProvider<*>>, PluginCapability> {
	val result = mutableMapOf<Class<out InstallCapabilityProvider<*>>, PluginCapability>()
	for (pluginClass in scope.getPlugins().keys) {
		val plugin = AckpinePluginCache.get(pluginClass)
		if (plugin is InstallCapabilityProvider<*>) {
			@Suppress("UNCHECKED_CAST")
			result[pluginClass as Class<InstallCapabilityProvider<*>>] = plugin.getCapabilities(context)
		}
	}
	return result
}

private fun normalizeInstallerType(type: InstallerType): InstallerType {
	if (!isPackageInstallerApiAvailable()) {
		return InstallerType.INTENT_BASED
	}
	return type
}

private class BaseInstallerCapabilities(
	val skipUserAction: CapabilityStatus,
	val preapproval: CapabilityStatus,
	val constraints: CapabilityStatus,
	val requestUpdateOwnership: CapabilityStatus,
	val packageSource: CapabilityStatus,
	val dontKillApp: CapabilityStatus
)