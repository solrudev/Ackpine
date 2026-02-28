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

package ru.solrudev.ackpine.installer.parameters

import android.net.Uri
import ru.solrudev.ackpine.DelicateAckpineApi
import ru.solrudev.ackpine.plugability.AckpinePlugin
import ru.solrudev.ackpine.plugability.TestParameterlessPlugin
import ru.solrudev.ackpine.plugability.TestPlugin
import ru.solrudev.ackpine.resources.ResolvableString
import ru.solrudev.ackpine.session.parameters.Confirmation
import ru.solrudev.ackpine.session.parameters.NotificationData
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InstallParametersBuilderTest {

	@Test
	fun buildWithDefaults() {
		val parameters = InstallParameters.Builder(Uri.EMPTY).build()
		assertEquals(InstallerType.SESSION_BASED, parameters.installerType)
		assertEquals(Confirmation.DEFERRED, parameters.confirmation)
		assertEquals("", parameters.name)
		assertTrue(parameters.requireUserAction)
		assertEquals(InstallMode.Full, parameters.installMode)
		assertEquals(InstallPreapproval.NONE, parameters.preapproval)
		assertEquals(InstallConstraints.NONE, parameters.constraints)
		assertFalse(parameters.requestUpdateOwnership)
		assertEquals(PackageSource.Unspecified, parameters.packageSource)
	}

	@Test
	fun singleApkRespectsExplicitInstallerType() {
		val parameters = InstallParameters.Builder(Uri.EMPTY)
			.setInstallerType(InstallerType.INTENT_BASED)
			.build()
		assertEquals(InstallerType.INTENT_BASED, parameters.installerType)
	}

	@Test
	fun multipleApksForceSessionBasedInstallerType() {
		val parameters = InstallParameters.Builder(listOf(Uri.EMPTY, Uri.EMPTY))
			.setInstallerType(InstallerType.INTENT_BASED)
			.build()
		assertEquals(InstallerType.SESSION_BASED, parameters.installerType)
	}

	@Test
	fun addApkEnforcesSessionBasedInstallerType() {
		val parameters = InstallParameters.Builder(Uri.EMPTY)
			.setInstallerType(InstallerType.INTENT_BASED)
			.addApk(Uri.EMPTY)
			.addApks(listOf(Uri.EMPTY, Uri.EMPTY))
			.build()
		assertEquals(InstallerType.SESSION_BASED, parameters.installerType)
		assertEquals(4, parameters.apks.size)
	}

	@Test
	fun iterableConstructorRejectsEmptyApkList() {
		assertFailsWith<IllegalArgumentException> {
			InstallParameters.Builder(emptyList())
		}
	}

	@OptIn(DelicateAckpineApi::class)
	@Test
	fun settersAreReflectedInBuiltParameters() {
		val notificationData = NotificationData.Builder()
			.setTitle(ResolvableString.raw("title"))
			.setContentText(ResolvableString.raw("text"))
			.build()
		val installMode = InstallMode.InheritExisting("com.example.pkg", dontKillApp = true)
		val preapproval = InstallPreapproval.Builder("com.example.pkg", "label", Locale.US).build()
		val constraints = InstallConstraints.Builder(1000L)
			.setAppNotForegroundRequired(true)
			.build()
		val parameters = InstallParameters.Builder(Uri.EMPTY)
			.setInstallerType(InstallerType.INTENT_BASED)
			.setConfirmation(Confirmation.IMMEDIATE)
			.setNotificationData(notificationData)
			.setName("test-app")
			.setRequireUserAction(false)
			.setInstallMode(installMode)
			.setPreapproval(preapproval)
			.setConstraints(constraints)
			.setRequestUpdateOwnership(true)
			.setPackageSource(PackageSource.Store)
			.build()
		assertEquals(InstallerType.INTENT_BASED, parameters.installerType)
		assertEquals(Confirmation.IMMEDIATE, parameters.confirmation)
		assertEquals(notificationData, parameters.notificationData)
		assertEquals("test-app", parameters.name)
		assertFalse(parameters.requireUserAction)
		assertEquals(installMode, parameters.installMode)
		assertEquals(preapproval, parameters.preapproval)
		assertEquals(constraints, parameters.constraints)
		assertTrue(parameters.requestUpdateOwnership)
		assertEquals(PackageSource.Store, parameters.packageSource)
	}

	@Test
	fun pluginIsAppliedDuringBuild() {
		val parameters = InstallParameters.Builder(Uri.EMPTY)
			.usePlugin(TestPlugin::class.java, TestPlugin.Parameters(""))
			.build()
		assertEquals("applied-by-plugin", parameters.name)
	}

	@Test
	fun parameterlessPluginIsAppliedDuringBuild() {
		val parameters = InstallParameters.Builder(Uri.EMPTY)
			.usePlugin(TestParameterlessPlugin::class.java)
			.build()
		assertEquals("applied-by-plugin", parameters.name)
	}

	@Test
	fun pluginParametersArePreserved() {
		val parameters = InstallParameters.Builder(Uri.EMPTY)
			.usePlugin(TestPlugin::class.java, TestPlugin.Parameters("value"))
			.build()
		val expectedPlugins = mapOf<Class<out AckpinePlugin<*>>, TestPlugin.Parameters>(
			TestPlugin::class.java to TestPlugin.Parameters("value")
		)
		assertEquals(expectedPlugins, parameters.pluginContainer.getPlugins())
	}
}