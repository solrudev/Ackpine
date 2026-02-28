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

import ru.solrudev.ackpine.plugability.AckpinePlugin
import ru.solrudev.ackpine.plugability.TestParameterlessPlugin
import ru.solrudev.ackpine.plugability.TestPlugin
import ru.solrudev.ackpine.session.parameters.Confirmation
import ru.solrudev.ackpine.session.parameters.NotificationData
import kotlin.test.Test
import kotlin.test.assertEquals

class UninstallParametersBuilderTest {

	@Test
	fun buildWithDefaults() {
		val parameters = UninstallParameters.Builder("com.example").build()
		assertEquals("com.example", parameters.packageName)
		assertEquals(UninstallerType.PACKAGE_INSTALLER_BASED, parameters.uninstallerType)
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
	fun pluginIsAppliedDuringBuild() {
		val parameters = UninstallParameters.Builder("com.example")
			.usePlugin(TestPlugin::class.java, TestPlugin.Parameters(""))
			.build()
		assertEquals("applied-by-plugin", parameters.packageName)
	}

	@Test
	fun parameterlessPluginIsAppliedDuringBuild() {
		val parameters = UninstallParameters.Builder("com.example")
			.usePlugin(TestParameterlessPlugin::class.java)
			.build()
		assertEquals("applied-by-plugin", parameters.packageName)
	}

	@Test
	fun pluginParametersArePreserved() {
		val parameters = UninstallParameters.Builder("com.example")
			.usePlugin(TestPlugin::class.java, TestPlugin.Parameters("value"))
			.build()
		val expectedPlugins = mapOf<Class<out AckpinePlugin<*>>, TestPlugin.Parameters>(
			TestPlugin::class.java to TestPlugin.Parameters("value")
		)
		assertEquals(expectedPlugins, parameters.pluginContainer.getPlugins())
	}
}