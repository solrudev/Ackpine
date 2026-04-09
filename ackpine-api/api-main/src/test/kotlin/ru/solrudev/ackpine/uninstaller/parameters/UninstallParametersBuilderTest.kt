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

package ru.solrudev.ackpine.uninstaller.parameters

import ru.solrudev.ackpine.SdkInt
import ru.solrudev.ackpine.plugability.AckpinePlugin
import ru.solrudev.ackpine.plugability.BackendFlipperPlugin
import ru.solrudev.ackpine.plugability.ChainedPlugin
import ru.solrudev.ackpine.plugability.ChainedTestPlugin
import ru.solrudev.ackpine.plugability.IntentBasedBackendObserverPlugin
import ru.solrudev.ackpine.plugability.LegacyChainedUninstallPlugin
import ru.solrudev.ackpine.plugability.LegacyUninstallPlugin
import ru.solrudev.ackpine.plugability.TestInstallPlugin
import ru.solrudev.ackpine.plugability.TestParameterlessPlugin
import ru.solrudev.ackpine.plugability.TestPlugin
import ru.solrudev.ackpine.session.parameters.Confirmation
import ru.solrudev.ackpine.session.parameters.NotificationData
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UninstallParametersBuilderTest {

	@AfterTest
	fun tearDown() = SdkInt.reset()

	@Test
	fun buildWithDefaults() {
		val parameters = UninstallParameters.Builder("com.example").build()
		assertEquals("com.example", parameters.packageName)
		assertEquals(UninstallerType.DEFAULT, parameters.uninstallerType)
		assertEquals(Confirmation.DEFERRED, parameters.confirmation)
		assertEquals(NotificationData.DEFAULT, parameters.notificationData)
	}

	@Test
	fun settersAreReflectedInBuiltParameters() {
		val notificationData = NotificationData.DEFAULT
		val parameters = UninstallParameters.Builder("com.example")
			.setPackageName("com.other")
			.setUninstallerType(UninstallerType.INTENT_BASED)
			.setConfirmation(Confirmation.IMMEDIATE)
			.setNotificationData(notificationData)
			.build()
		assertEquals("com.other", parameters.packageName)
		assertEquals(UninstallerType.INTENT_BASED, parameters.uninstallerType)
		assertEquals(Confirmation.IMMEDIATE, parameters.confirmation)
		assertEquals(notificationData, parameters.notificationData)
	}

	@Test
	fun lowApiEnforcesIntentBasedUninstallerType() {
		SdkInt.set(19)
		val parameters = UninstallParameters.Builder("com.example")
			.setUninstallerType(UninstallerType.PACKAGE_INSTALLER_BASED)
			.build()
		assertEquals(UninstallerType.INTENT_BASED, parameters.uninstallerType)
	}

	@Suppress("DEPRECATION")
	@Test
	fun pluginIsAppliedDuringBuild() {
		val expectedPlugins = mapOf<Class<out AckpinePlugin>, AckpinePlugin.Parameters>(
			TestPlugin::class.java to TestPlugin.Parameters("")
		)
		val parameters = UninstallParameters.Builder("com.example")
			.registerPlugin(TestPlugin::class.java, TestPlugin.Parameters(""))
			.build()
		assertEquals(expectedPlugins, parameters.pluginContainer.getPlugins())

		val deprecatedParameters = UninstallParameters.Builder("com.example")
			.usePlugin(TestPlugin::class.java, TestPlugin.Parameters(""))
			.build()
		assertEquals(expectedPlugins, deprecatedParameters.pluginContainer.getPlugins())
	}

	@Suppress("DEPRECATION")
	@Test
	fun parameterlessPluginIsAppliedDuringBuild() {
		val expectedPlugins = mapOf<Class<out AckpinePlugin>, AckpinePlugin.Parameters>(
			TestParameterlessPlugin::class.java to AckpinePlugin.Parameters.None
		)
		val parameters = UninstallParameters.Builder("com.example")
			.registerPlugin(TestParameterlessPlugin::class.java)
			.build()
		assertEquals(expectedPlugins, parameters.pluginContainer.getPlugins())

		val deprecatedParameters = UninstallParameters.Builder("com.example")
			.usePlugin(TestParameterlessPlugin::class.java)
			.build()
		assertEquals(expectedPlugins, deprecatedParameters.pluginContainer.getPlugins())
	}

	@Suppress("DEPRECATION")
	@Test
	fun chainedPluginIsAppliedDuringBuild() {
		val parameters = UninstallParameters.Builder("com.example")
			.registerPlugin(ChainedTestPlugin::class.java)
			.build()
		val expectedPlugins = mapOf<Class<out AckpinePlugin>, AckpinePlugin.Parameters>(
			ChainedTestPlugin::class.java to AckpinePlugin.Parameters.None,
			ChainedPlugin::class.java to AckpinePlugin.Parameters.None,
			TestParameterlessPlugin::class.java to AckpinePlugin.Parameters.None
		)
		assertEquals(expectedPlugins, parameters.pluginContainer.getPlugins())

		val deprecatedParameters = UninstallParameters.Builder("com.example")
			.usePlugin(ChainedTestPlugin::class.java)
			.build()
		assertEquals(expectedPlugins, deprecatedParameters.pluginContainer.getPlugins())
	}

	@Suppress("DEPRECATION")
	@Test
	fun pluginParametersArePreserved() {
		val parameters = UninstallParameters.Builder("com.example")
			.registerPlugin(TestPlugin::class.java, TestPlugin.Parameters("value"))
			.build()
		val expectedPlugins = mapOf<Class<out AckpinePlugin>, TestPlugin.Parameters>(
			TestPlugin::class.java to TestPlugin.Parameters("value")
		)
		assertEquals(expectedPlugins, parameters.pluginContainer.getPlugins())

		val deprecatedParameters = UninstallParameters.Builder("com.example")
			.usePlugin(TestPlugin::class.java, TestPlugin.Parameters("value"))
			.build()
		assertEquals(expectedPlugins, deprecatedParameters.pluginContainer.getPlugins())
	}

	@Test
	fun buildIsIdempotent() {
		val builder = UninstallParameters.Builder("com.example")
			.registerPlugin(ChainedTestPlugin::class.java)
			.registerPlugin(LegacyUninstallPlugin::class.java)
		val first = builder.build()
		val second = builder.build()
		assertEquals(first, second)
	}

	@Test
	@Suppress("DEPRECATION")
	fun legacyPluginIsAppliedDuringBuild() {
		val parameters = UninstallParameters.Builder("com.example")
			.registerPlugin(LegacyUninstallPlugin::class.java)
			.build()
		assertEquals("com.legacy", parameters.packageName)
	}

	@Test
	@Suppress("DEPRECATION")
	fun legacyChainedPluginIsAppliedDuringBuild() {
		val parameters = UninstallParameters.Builder("com.example")
			.registerPlugin(LegacyChainedUninstallPlugin::class.java)
			.build()
		val expectedPlugins = mapOf<Class<out AckpinePlugin>, AckpinePlugin.Parameters>(
			LegacyChainedUninstallPlugin::class.java to AckpinePlugin.Parameters.None,
			TestPlugin::class.java to TestPlugin.Parameters("")
		)
		assertEquals(expectedPlugins, parameters.pluginContainer.getPlugins())
	}

	@Test
	fun transitivePluginObservesNormalizedUninstallerType() {
		SdkInt.set(16)
		val parameters = UninstallParameters.Builder("com.example")
			.registerPlugin(BackendFlipperPlugin::class.java)
			.build()
		val expectedPlugins = mapOf<Class<out AckpinePlugin>, AckpinePlugin.Parameters>(
			BackendFlipperPlugin::class.java to AckpinePlugin.Parameters.None,
			IntentBasedBackendObserverPlugin::class.java to AckpinePlugin.Parameters.None,
			TestParameterlessPlugin::class.java to AckpinePlugin.Parameters.None // applied by observer on intent-based
		)
		assertEquals(UninstallerType.INTENT_BASED, parameters.uninstallerType)
		assertEquals(expectedPlugins, parameters.pluginContainer.getPlugins())
	}

	@Test
	@Suppress("DEPRECATION")
	fun deprecatedUsePluginWithInstallPluginThrows() {
		assertFailsWith<IllegalStateException> {
			UninstallParameters.Builder("com.example")
				.usePlugin(TestInstallPlugin::class.java)
		}
		assertFailsWith<IllegalStateException> {
			UninstallParameters.Builder("com.example")
				.usePlugin(TestInstallPlugin::class.java, AckpinePlugin.Parameters.None)
		}
	}
}