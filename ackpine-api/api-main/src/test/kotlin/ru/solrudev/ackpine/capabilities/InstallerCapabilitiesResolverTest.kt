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
import ru.solrudev.ackpine.installer.parameters.InstallerType
import ru.solrudev.ackpine.plugability.AckpineInstallPlugin
import ru.solrudev.ackpine.plugability.BackendFlipperPlugin
import ru.solrudev.ackpine.plugability.CapabilityAwareInstallPlugin
import ru.solrudev.ackpine.plugability.CapabilityRegistrarPlugin
import ru.solrudev.ackpine.plugability.ChainedTestPlugin
import ru.solrudev.ackpine.plugability.DisableConstraintsPlugin
import ru.solrudev.ackpine.plugability.DisablePreapprovalPlugin
import ru.solrudev.ackpine.plugability.DisableUpdateOwnershipPlugin
import ru.solrudev.ackpine.plugability.ForceUserActionPlugin
import ru.solrudev.ackpine.plugability.LegacyInstallPlugin
import ru.solrudev.ackpine.plugability.TestInstallCapability
import ru.solrudev.ackpine.plugability.TestParameterlessPlugin
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InstallerCapabilitiesResolverTest {

	@AfterTest
	fun tearDown() = SdkInt.reset()

	@Test
	fun sessionBasedApi34AllBaseCapabilitiesSupported() {
		SdkInt.set(34)
		val caps = resolve(InstallerType.SESSION_BASED)
		assertEquals(InstallerType.SESSION_BASED, caps.installerType)
		assertEquals(CapabilityStatus.UNRELIABLE, caps.skipUserAction)
		assertEquals(CapabilityStatus.SUPPORTED, caps.preapproval)
		assertEquals(CapabilityStatus.SUPPORTED, caps.constraints)
		assertEquals(CapabilityStatus.SUPPORTED, caps.requestUpdateOwnership)
		assertEquals(CapabilityStatus.SUPPORTED, caps.packageSource)
		assertEquals(CapabilityStatus.SUPPORTED, caps.dontKillApp)
	}

	@Test
	fun intentBasedAllCapabilitiesUnsupported() {
		SdkInt.set(34)
		val caps = resolve(InstallerType.INTENT_BASED)
		assertEquals(InstallerType.INTENT_BASED, caps.installerType)
		assertEquals(CapabilityStatus.UNSUPPORTED, caps.skipUserAction)
		assertEquals(CapabilityStatus.UNSUPPORTED, caps.preapproval)
		assertEquals(CapabilityStatus.UNSUPPORTED, caps.constraints)
		assertEquals(CapabilityStatus.UNSUPPORTED, caps.requestUpdateOwnership)
		assertEquals(CapabilityStatus.UNSUPPORTED, caps.packageSource)
		assertEquals(CapabilityStatus.UNSUPPORTED, caps.dontKillApp)
	}

	@Test
	fun sessionBasedApi20NormalizedToIntentBased() {
		SdkInt.set(20)
		val caps = resolve(InstallerType.SESSION_BASED)
		assertEquals(InstallerType.INTENT_BASED, caps.installerType)
		assertEquals(CapabilityStatus.UNSUPPORTED, caps.skipUserAction)
		assertEquals(CapabilityStatus.UNSUPPORTED, caps.preapproval)
		assertEquals(CapabilityStatus.UNSUPPORTED, caps.constraints)
		assertEquals(CapabilityStatus.UNSUPPORTED, caps.requestUpdateOwnership)
		assertEquals(CapabilityStatus.UNSUPPORTED, caps.packageSource)
		assertEquals(CapabilityStatus.UNSUPPORTED, caps.dontKillApp)
	}

	@Test
	fun sessionBasedApi34FeaturesAreUnsupportedOnOlderVersions() {
		SdkInt.set(33)
		val caps = resolve(InstallerType.SESSION_BASED)
		assertEquals(CapabilityStatus.UNSUPPORTED, caps.constraints)
		assertEquals(CapabilityStatus.UNSUPPORTED, caps.preapproval)
		assertEquals(CapabilityStatus.UNSUPPORTED, caps.requestUpdateOwnership)
		assertEquals(CapabilityStatus.UNSUPPORTED, caps.dontKillApp)
	}

	@Test
	fun sessionBasedApi33PackageSourceSupported() {
		SdkInt.set(33)
		val caps = resolve(InstallerType.SESSION_BASED)
		assertEquals(CapabilityStatus.SUPPORTED, caps.packageSource)
	}

	@Test
	fun sessionBasedPreApi33PackageSourceUnsupported() {
		SdkInt.set(32)
		val caps = resolve(InstallerType.SESSION_BASED)
		assertEquals(CapabilityStatus.UNSUPPORTED, caps.packageSource)
	}

	@Test
	fun sessionBasedPreApi31RequireUserActionIsUnsupported() {
		SdkInt.set(30)
		val caps = resolve(InstallerType.SESSION_BASED)
		assertEquals(CapabilityStatus.UNSUPPORTED, caps.skipUserAction)
	}

	@Test
	fun pluginForcesUserActionReportedAsUnsupported() {
		SdkInt.set(34)
		val caps = resolve(InstallerType.SESSION_BASED, ForceUserActionPlugin::class.java)
		assertEquals(CapabilityStatus.UNSUPPORTED, caps.skipUserAction)
		assertEquals(CapabilityStatus.SUPPORTED, caps.preapproval)
		assertEquals(CapabilityStatus.SUPPORTED, caps.constraints)
		assertEquals(CapabilityStatus.SUPPORTED, caps.requestUpdateOwnership)
	}

	@Test
	fun pluginDisablesPreapprovalReportedAsUnsupported() {
		SdkInt.set(34)
		val caps = resolve(InstallerType.SESSION_BASED, DisablePreapprovalPlugin::class.java)
		assertEquals(CapabilityStatus.UNSUPPORTED, caps.preapproval)
		assertEquals(CapabilityStatus.UNRELIABLE, caps.skipUserAction)
		assertEquals(CapabilityStatus.SUPPORTED, caps.constraints)
		assertEquals(CapabilityStatus.SUPPORTED, caps.requestUpdateOwnership)
	}

	@Test
	fun pluginDisablesConstraintsReportedAsUnsupported() {
		SdkInt.set(34)
		val caps = resolve(InstallerType.SESSION_BASED, DisableConstraintsPlugin::class.java)
		assertEquals(CapabilityStatus.UNSUPPORTED, caps.constraints)
		assertEquals(CapabilityStatus.UNRELIABLE, caps.skipUserAction)
		assertEquals(CapabilityStatus.SUPPORTED, caps.preapproval)
		assertEquals(CapabilityStatus.SUPPORTED, caps.requestUpdateOwnership)
	}

	@Test
	fun pluginDisablesRequestUpdateOwnershipReportedAsUnsupported() {
		SdkInt.set(34)
		val caps = resolve(InstallerType.SESSION_BASED, DisableUpdateOwnershipPlugin::class.java)
		assertEquals(CapabilityStatus.UNSUPPORTED, caps.requestUpdateOwnership)
		assertEquals(CapabilityStatus.UNRELIABLE, caps.skipUserAction)
		assertEquals(CapabilityStatus.SUPPORTED, caps.preapproval)
		assertEquals(CapabilityStatus.SUPPORTED, caps.constraints)
	}

	@Test
	fun transitivePluginAffectsCapabilities() {
		SdkInt.set(34)
		// ChainedTestPlugin -> ChainedPlugin -> TestParameterlessPlugin (disablePreapproval)
		val caps = resolve(InstallerType.SESSION_BASED, ChainedTestPlugin::class.java)
		assertEquals(CapabilityStatus.UNSUPPORTED, caps.preapproval)
	}

	@Test
	fun backendFlipperPluginFlipsToIntentBased() {
		SdkInt.set(34)
		// BackendFlipperPlugin flips SESSION_BASED -> INTENT_BASED
		val caps = resolve(InstallerType.SESSION_BASED, BackendFlipperPlugin::class.java)
		assertEquals(InstallerType.INTENT_BASED, caps.installerType)
		assertEquals(CapabilityStatus.UNSUPPORTED, caps.skipUserAction)
		assertEquals(CapabilityStatus.UNSUPPORTED, caps.preapproval)
		assertEquals(CapabilityStatus.UNSUPPORTED, caps.constraints)
		assertEquals(CapabilityStatus.UNSUPPORTED, caps.requestUpdateOwnership)
		assertEquals(CapabilityStatus.UNSUPPORTED, caps.packageSource)
		assertEquals(CapabilityStatus.UNSUPPORTED, caps.dontKillApp)
	}

	@Test
	fun backendFlipperPluginFlipsToSessionBased() {
		SdkInt.set(34)
		// BackendFlipperPlugin flips INTENT_BASED -> SESSION_BASED
		val caps = resolve(InstallerType.INTENT_BASED, BackendFlipperPlugin::class.java)
		assertEquals(InstallerType.SESSION_BASED, caps.installerType)
		assertEquals(CapabilityStatus.UNRELIABLE, caps.skipUserAction)
		assertEquals(CapabilityStatus.SUPPORTED, caps.preapproval)
		assertEquals(CapabilityStatus.SUPPORTED, caps.constraints)
		assertEquals(CapabilityStatus.SUPPORTED, caps.requestUpdateOwnership)
		assertEquals(CapabilityStatus.SUPPORTED, caps.packageSource)
		assertEquals(CapabilityStatus.SUPPORTED, caps.dontKillApp)
	}

	@Test
	fun capabilityAwarePluginReturnsPluginCapability() {
		SdkInt.set(34)
		val caps = resolve(InstallerType.SESSION_BASED, CapabilityAwareInstallPlugin::class.java)
		val pluginCap = caps.plugin(CapabilityAwareInstallPlugin::class.java)
		assertEquals(TestInstallCapability(InstallerType.SESSION_BASED), pluginCap)
	}

	@Test
	fun transitiveCapabilityAwarePluginReturnsPluginCapability() {
		SdkInt.set(34)
		// CapabilityRegistrarPlugin transitively registers CapabilityAwareInstallPlugin
		val caps = resolve(InstallerType.SESSION_BASED, CapabilityRegistrarPlugin::class.java)
		val pluginCap = caps.plugin(CapabilityAwareInstallPlugin::class.java)
		assertEquals(TestInstallCapability(InstallerType.SESSION_BASED), pluginCap)
	}

	@Test
	fun pluginNotInGraphReturnsNull() {
		SdkInt.set(34)
		val caps = resolve(InstallerType.SESSION_BASED)
		assertNull(caps.plugin(CapabilityAwareInstallPlugin::class.java))
	}

	@Test
	fun nonCapabilityPluginLookupReturnsNull() {
		SdkInt.set(34)
		// TestParameterlessPlugin does not implement InstallCapabilityProvider
		val caps = resolve(InstallerType.SESSION_BASED, TestParameterlessPlugin::class.java)
		assertNull(caps.plugin(CapabilityAwareInstallPlugin::class.java))
	}

	@Test
	fun legacyPluginIgnoredByCapabilityResolver() {
		SdkInt.set(34)
		// LegacyInstallPlugin overrides apply(builder) only
		val caps = resolve(InstallerType.SESSION_BASED, LegacyInstallPlugin::class.java)
		assertEquals(CapabilityStatus.UNRELIABLE, caps.skipUserAction)
	}

	private fun resolve(
		installerType: InstallerType,
		vararg pluginClasses: Class<out AckpineInstallPlugin<*>>
	): InstallerCapabilities {
		return resolveInstallerCapabilities(installerType, pluginClasses.asList())
	}
}