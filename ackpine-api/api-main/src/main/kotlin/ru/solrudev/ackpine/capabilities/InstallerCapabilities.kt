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

import ru.solrudev.ackpine.installer.PackageInstaller
import ru.solrudev.ackpine.installer.parameters.InstallParameters
import ru.solrudev.ackpine.installer.parameters.InstallMode
import ru.solrudev.ackpine.installer.parameters.InstallerType
import ru.solrudev.ackpine.plugability.AckpineInstallPlugin

/**
 * Capabilities of a [PackageInstaller] configuration as determined by [PackageInstaller.getCapabilities].
 *
 * The [installerType] field reflects the effective backend after normalization and plugin resolution, which may differ
 * from the requested value (e.g., when the requested backend is not supported by the current Android version).
 *
 * **Note:** Install capabilities are not split-aware. The capabilities API does not accept APK count or split
 * metadata, so the split-install invariant that forces [InstallerType.SESSION_BASED] is not reflected here.
 * If split APKs are in use, [InstallerType.SESSION_BASED] will be enforced at session creation time regardless of
 * what [getCapabilities][PackageInstaller.getCapabilities] reports for a given requested type.
 */
public class InstallerCapabilities internal constructor(

	/**
	 * The effective installer type after normalization and plugin resolution.
	 */
	public val installerType: InstallerType,

	/**
	 * Whether skipping user confirmation is available via modifying
	 * [requireUserAction][InstallParameters.requireUserAction].
	 *
	 * [CapabilityStatus.UNRELIABLE] indicates the feature is technically available but may behave differently across
	 * devices or Android versions. [CapabilityStatus.UNSUPPORTED] when the effective backend is
	 * [InstallerType.INTENT_BASED] or a plugin has forced user action.
	 */
	public val skipUserAction: CapabilityStatus,

	/**
	 * Whether the pre-commit install approval flow is supported.
	 *
	 * [CapabilityStatus.SUPPORTED] only on API level >= 34 with [InstallerType.SESSION_BASED]. Reported as
	 * [CapabilityStatus.UNSUPPORTED] if a plugin has disabled preapproval.
	 */
	public val preapproval: CapabilityStatus,

	/**
	 * Whether installation constraints are supported.
	 *
	 * [CapabilityStatus.SUPPORTED] only on API level >= 34 with [InstallerType.SESSION_BASED]. Reported as
	 * [CapabilityStatus.UNSUPPORTED] if a plugin has disabled constraints.
	 */
	public val constraints: CapabilityStatus,

	/**
	 * Whether requesting update ownership enforcement is supported.
	 *
	 * [CapabilityStatus.SUPPORTED] only on API level >= 34 with [InstallerType.SESSION_BASED]. Reported as
	 * [CapabilityStatus.UNSUPPORTED] if a plugin has disabled it.
	 */
	public val requestUpdateOwnership: CapabilityStatus,

	/**
	 * Whether setting the package source is supported.
	 *
	 * [CapabilityStatus.SUPPORTED] only on API level >= 33 with [InstallerType.SESSION_BASED].
	 */
	public val packageSource: CapabilityStatus,

	/**
	 * Whether [InstallMode.InheritExisting.dontKillApp] is supported.
	 *
	 * [CapabilityStatus.SUPPORTED] only on API level >= 34 with [InstallerType.SESSION_BASED].
	 */
	public val dontKillApp: CapabilityStatus,

	private val pluginCapabilities: Map<Class<out InstallCapabilityProvider<*>>, PluginCapability>
) {

	/**
	 * Returns the plugin-specific install capabilities for the given [pluginClass], or `null` if the plugin was not
	 * in the resolved graph.
	 * @param pluginClass Java class of the plugin, implementing both [AckpineInstallPlugin] and
	 * [InstallCapabilityProvider].
	 */
	public fun <Plugin, C> plugin(pluginClass: Class<out Plugin>): C?
			where Plugin : AckpineInstallPlugin<*>,
				  Plugin : InstallCapabilityProvider<C>,
				  C : PluginCapability {
		@Suppress("UNCHECKED_CAST")
		return pluginCapabilities[pluginClass] as C?
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is InstallerCapabilities) return false
		if (installerType != other.installerType) return false
		if (skipUserAction != other.skipUserAction) return false
		if (preapproval != other.preapproval) return false
		if (constraints != other.constraints) return false
		if (requestUpdateOwnership != other.requestUpdateOwnership) return false
		if (packageSource != other.packageSource) return false
		if (dontKillApp != other.dontKillApp) return false
		if (pluginCapabilities != other.pluginCapabilities) return false
		return true
	}

	override fun hashCode(): Int {
		var result = installerType.hashCode()
		result = 31 * result + skipUserAction.hashCode()
		result = 31 * result + preapproval.hashCode()
		result = 31 * result + constraints.hashCode()
		result = 31 * result + requestUpdateOwnership.hashCode()
		result = 31 * result + packageSource.hashCode()
		result = 31 * result + dontKillApp.hashCode()
		result = 31 * result + pluginCapabilities.hashCode()
		return result
	}

	override fun toString(): String {
		return "InstallerCapabilities(" +
				"installerType=$installerType, " +
				"skipUserAction=$skipUserAction, " +
				"preapproval=$preapproval, " +
				"constraints=$constraints, " +
				"requestUpdateOwnership=$requestUpdateOwnership, " +
				"packageSource=$packageSource, " +
				"dontKillApp=$dontKillApp, " +
				"pluginCapabilities=$pluginCapabilities" +
				")"
	}
}