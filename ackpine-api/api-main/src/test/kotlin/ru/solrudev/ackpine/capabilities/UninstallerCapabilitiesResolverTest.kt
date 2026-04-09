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

import ru.solrudev.ackpine.SdkInt
import ru.solrudev.ackpine.plugability.AckpineUninstallPlugin
import ru.solrudev.ackpine.plugability.BackendFlipperPlugin
import ru.solrudev.ackpine.plugability.CapabilityAwareUninstallPlugin
import ru.solrudev.ackpine.plugability.CapabilityRegistrarUninstallPlugin
import ru.solrudev.ackpine.plugability.LegacyUninstallPlugin
import ru.solrudev.ackpine.plugability.TestUninstallCapability
import ru.solrudev.ackpine.uninstaller.parameters.UninstallerType
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UninstallerCapabilitiesResolverTest {

	@AfterTest
	fun tearDown() = SdkInt.reset()

	@Test
	fun packageInstallerBasedApi34EffectiveType() {
		SdkInt.set(34)
		val caps = resolve(UninstallerType.PACKAGE_INSTALLER_BASED)
		assertEquals(UninstallerType.PACKAGE_INSTALLER_BASED, caps.uninstallerType)
	}

	@Test
	fun intentBasedApi34EffectiveType() {
		SdkInt.set(34)
		val caps = resolve(UninstallerType.INTENT_BASED)
		assertEquals(UninstallerType.INTENT_BASED, caps.uninstallerType)
	}

	@Test
	fun packageInstallerBasedApi20NormalizedToIntentBased() {
		SdkInt.set(20)
		val caps = resolve(UninstallerType.PACKAGE_INSTALLER_BASED)
		assertEquals(UninstallerType.INTENT_BASED, caps.uninstallerType)
	}

	@Test
	fun pluginOverridesBackendEffectiveTypeReflectsOverride() {
		SdkInt.set(34)
		// BackendFlipperPlugin flips PACKAGE_INSTALLER_BASED -> INTENT_BASED
		val caps1 = resolve(UninstallerType.PACKAGE_INSTALLER_BASED, BackendFlipperPlugin::class.java)
		assertEquals(UninstallerType.INTENT_BASED, caps1.uninstallerType)
		// BackendFlipperPlugin flips INTENT_BASED -> PACKAGE_INSTALLER_BASED
		val caps2 = resolve(UninstallerType.INTENT_BASED, BackendFlipperPlugin::class.java)
		assertEquals(UninstallerType.PACKAGE_INSTALLER_BASED, caps2.uninstallerType)
	}

	@Test
	fun capabilityAwarePluginReturnsPluginCapability() {
		SdkInt.set(34)
		val caps = resolve(UninstallerType.PACKAGE_INSTALLER_BASED, CapabilityAwareUninstallPlugin::class.java)
		val pluginCap = caps.plugin(CapabilityAwareUninstallPlugin::class.java)
		assertEquals(TestUninstallCapability(UninstallerType.PACKAGE_INSTALLER_BASED), pluginCap)
	}

	@Test
	fun transitiveCapabilityAwarePluginReturnsPluginCapability() {
		SdkInt.set(34)
		// CapabilityRegistrarUninstallPlugin -> CapabilityAwareUninstallPlugin
		val caps = resolve(UninstallerType.PACKAGE_INSTALLER_BASED, CapabilityRegistrarUninstallPlugin::class.java)
		val pluginCap = caps.plugin(CapabilityAwareUninstallPlugin::class.java)
		assertEquals(TestUninstallCapability(UninstallerType.PACKAGE_INSTALLER_BASED), pluginCap)
	}

	@Test
	fun pluginNotInGraphReturnsNull() {
		SdkInt.set(34)
		val caps = resolve(UninstallerType.PACKAGE_INSTALLER_BASED)
		assertNull(caps.plugin(CapabilityAwareUninstallPlugin::class.java))
	}

	@Test
	fun legacyPluginIgnoredByCapabilities() {
		SdkInt.set(34)
		// LegacyUninstallPlugin overrides apply(builder) only
		val caps = resolve(UninstallerType.PACKAGE_INSTALLER_BASED, LegacyUninstallPlugin::class.java)
		assertEquals(UninstallerType.PACKAGE_INSTALLER_BASED, caps.uninstallerType)
	}

	private fun resolve(
		uninstallerType: UninstallerType,
		vararg pluginClasses: Class<out AckpineUninstallPlugin<*>>
	) = resolveUninstallerCapabilities(uninstallerType, pluginClasses.asList())
}