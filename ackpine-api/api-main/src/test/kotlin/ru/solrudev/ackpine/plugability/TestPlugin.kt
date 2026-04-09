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

package ru.solrudev.ackpine.plugability

import ru.solrudev.ackpine.DelicateAckpineApi
import ru.solrudev.ackpine.capabilities.InstallCapabilityContext
import ru.solrudev.ackpine.capabilities.InstallCapabilityProvider
import ru.solrudev.ackpine.capabilities.PluginCapability
import ru.solrudev.ackpine.capabilities.UninstallCapabilityContext
import ru.solrudev.ackpine.capabilities.UninstallCapabilityProvider
import ru.solrudev.ackpine.installer.parameters.InstallParameters
import ru.solrudev.ackpine.installer.parameters.InstallerType
import ru.solrudev.ackpine.uninstaller.parameters.UninstallParameters
import ru.solrudev.ackpine.uninstaller.parameters.UninstallerType

class TestPlugin :
	AckpineInstallPlugin<TestPlugin.Parameters>,
	AckpineUninstallPlugin<TestPlugin.Parameters> {

	override val id = "test-plugin"

	override fun apply(scope: InstallPluginScope) {
		scope.requireUserAction = false
	}

	data class Parameters(val value: String) : AckpinePlugin.Parameters
}

class TestParameterlessPlugin :
	AckpineInstallPlugin<AckpinePlugin.Parameters.None>,
	AckpineUninstallPlugin<AckpinePlugin.Parameters.None> {

	override val id = "test-parameterless-plugin"

	override fun apply(scope: InstallPluginScope) {
		scope.requireUserAction = false
		scope.disablePreapproval()
	}
}

class ForceUserActionPlugin : AckpineInstallPlugin<AckpinePlugin.Parameters.None> {

	override val id = "force-user-action-plugin"

	override fun apply(scope: InstallPluginScope) {
		scope.requireUserAction = true
	}
}

class ChainedTestPlugin :
	AckpineInstallPlugin<AckpinePlugin.Parameters.None>,
	AckpineUninstallPlugin<AckpinePlugin.Parameters.None> {

	override val id = "chained-test-plugin"

	override fun apply(scope: InstallPluginScope) {
		scope.registerPlugin(ChainedPlugin::class.java)
	}

	override fun apply(scope: UninstallPluginScope) {
		scope.registerPlugin(ChainedPlugin::class.java)
	}
}

class ChainedPlugin :
	AckpineInstallPlugin<AckpinePlugin.Parameters.None>,
	AckpineUninstallPlugin<AckpinePlugin.Parameters.None> {

	override val id = "chained-plugin"

	override fun apply(scope: InstallPluginScope) {
		scope.registerPlugin(TestParameterlessPlugin::class.java)
	}

	override fun apply(scope: UninstallPluginScope) {
		scope.registerPlugin(TestParameterlessPlugin::class.java)
	}
}

class TestInstallPlugin : AckpineInstallPlugin<AckpinePlugin.Parameters.None> {
	override val id = "test-install-plugin"
}

class TestUninstallPlugin : AckpineUninstallPlugin<AckpinePlugin.Parameters.None> {
	override val id = "test-uninstall-plugin"
}

@Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
class LegacyInstallPlugin : AckpineInstallPlugin<AckpinePlugin.Parameters.None> {

	override val id = "legacy-install-plugin"

	@OptIn(DelicateAckpineApi::class)
	override fun apply(builder: InstallParameters.Builder) {
		builder.setRequireUserAction(false)
		builder.setName("legacy")
	}
}

@Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
class LegacyUninstallPlugin : AckpineUninstallPlugin<AckpinePlugin.Parameters.None> {

	override val id = "legacy-uninstall-plugin"

	override fun apply(builder: UninstallParameters.Builder) {
		builder.setPackageName("com.legacy")
	}
}

@Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
class LegacyChainedInstallPlugin : AckpineInstallPlugin<AckpinePlugin.Parameters.None> {

	override val id = "legacy-chained-install-plugin"

	override fun apply(builder: InstallParameters.Builder) {
		builder.registerPlugin(TestParameterlessPlugin::class.java)
	}
}

@Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
class LegacyChainedUninstallPlugin : AckpineUninstallPlugin<AckpinePlugin.Parameters.None> {

	override val id = "legacy-chained-uninstall-plugin"

	override fun apply(builder: UninstallParameters.Builder) {
		builder.registerPlugin(TestPlugin::class.java, TestPlugin.Parameters(""))
	}
}

class BackendFlipperPlugin :
	AckpineInstallPlugin<AckpinePlugin.Parameters.None>,
	AckpineUninstallPlugin<AckpinePlugin.Parameters.None> {

	override val id = "sets-intent-based-plugin"

	override fun apply(scope: InstallPluginScope) {
		scope.installerType = when (scope.installerType) {
			InstallerType.INTENT_BASED -> InstallerType.SESSION_BASED
			InstallerType.SESSION_BASED -> InstallerType.INTENT_BASED
		}
		scope.registerPlugin(IntentBasedBackendObserverPlugin::class.java)
	}

	override fun apply(scope: UninstallPluginScope) {
		scope.uninstallerType = when (scope.uninstallerType) {
			UninstallerType.INTENT_BASED -> UninstallerType.PACKAGE_INSTALLER_BASED
			UninstallerType.PACKAGE_INSTALLER_BASED -> UninstallerType.INTENT_BASED
		}
		scope.registerPlugin(IntentBasedBackendObserverPlugin::class.java)
	}
}

data class TestInstallCapability(val installerType: InstallerType) : PluginCapability
data class TestUninstallCapability(val uninstallerType: UninstallerType) : PluginCapability

class CapabilityAwareInstallPlugin :
	AckpineInstallPlugin<AckpinePlugin.Parameters.None>,
	InstallCapabilityProvider<TestInstallCapability> {

	override val id = "capability-aware-install-plugin"

	override fun getCapabilities(context: InstallCapabilityContext): TestInstallCapability {
		return TestInstallCapability(context.installerType)
	}
}

class CapabilityAwareUninstallPlugin :
	AckpineUninstallPlugin<AckpinePlugin.Parameters.None>,
	UninstallCapabilityProvider<TestUninstallCapability> {

	override val id = "capability-aware-uninstall-plugin"

	override fun getCapabilities(context: UninstallCapabilityContext): TestUninstallCapability {
		return TestUninstallCapability(context.uninstallerType)
	}
}

class DisablePreapprovalPlugin : AckpineInstallPlugin<AckpinePlugin.Parameters.None> {

	override val id = "preapproval-disabler"

	override fun apply(scope: InstallPluginScope) {
		scope.disablePreapproval()
	}
}

class DisableConstraintsPlugin : AckpineInstallPlugin<AckpinePlugin.Parameters.None> {

	override val id = "constraints-disabler"

	override fun apply(scope: InstallPluginScope) {
		scope.disableConstraints()
	}
}

class DisableUpdateOwnershipPlugin : AckpineInstallPlugin<AckpinePlugin.Parameters.None> {

	override val id = "update-ownership-disabler"

	override fun apply(scope: InstallPluginScope) {
		scope.requestUpdateOwnership = false
	}
}

class CapabilityRegistrarPlugin : AckpineInstallPlugin<AckpinePlugin.Parameters.None> {

	override val id = "capability-registrar-plugin"

	override fun apply(scope: InstallPluginScope) {
		scope.registerPlugin(CapabilityAwareInstallPlugin::class.java)
	}
}

class CapabilityRegistrarUninstallPlugin : AckpineUninstallPlugin<AckpinePlugin.Parameters.None> {

	override val id = "capability-registrar-uninstall-plugin"

	override fun apply(scope: UninstallPluginScope) {
		scope.registerPlugin(CapabilityAwareUninstallPlugin::class.java)
	}
}

class IntentBasedBackendObserverPlugin :
	AckpineInstallPlugin<AckpinePlugin.Parameters.None>,
	AckpineUninstallPlugin<AckpinePlugin.Parameters.None> {

	override val id = "observes-backend-type-plugin"

	override fun apply(scope: InstallPluginScope) {
		if (scope.installerType == InstallerType.INTENT_BASED) {
			scope.registerPlugin(TestParameterlessPlugin::class.java)
		}
	}

	override fun apply(scope: UninstallPluginScope) {
		if (scope.uninstallerType == UninstallerType.INTENT_BASED) {
			scope.registerPlugin(TestParameterlessPlugin::class.java)
		}
	}
}