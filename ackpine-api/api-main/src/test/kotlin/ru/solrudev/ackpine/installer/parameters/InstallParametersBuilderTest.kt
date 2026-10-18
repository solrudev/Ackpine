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
import ru.solrudev.ackpine.SdkInt
import ru.solrudev.ackpine.exceptions.SplitPackagesNotSupportedException
import ru.solrudev.ackpine.plugability.AckpinePlugin
import ru.solrudev.ackpine.plugability.BackendFlipperPlugin
import ru.solrudev.ackpine.plugability.ChainedPlugin
import ru.solrudev.ackpine.plugability.ChainedTestPlugin
import ru.solrudev.ackpine.plugability.IntentBasedBackendObserverPlugin
import ru.solrudev.ackpine.plugability.TestParameterlessPlugin
import ru.solrudev.ackpine.plugability.TestPlugin
import ru.solrudev.ackpine.resources.ResolvableString
import ru.solrudev.ackpine.session.parameters.Confirmation
import ru.solrudev.ackpine.session.parameters.NotificationData
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class InstallParametersBuilderTest {

	@AfterTest
	fun tearDown() = SdkInt.reset()

	@Test
	fun buildWithDefaults() {
		val parameters = InstallParameters.Builder(Uri.EMPTY).build()
		assertEquals(InstallerType.DEFAULT, parameters.installerType)
		assertEquals(Confirmation.DEFERRED, parameters.confirmation)
		assertEquals("", parameters.name)
		assertTrue(parameters.requireUserAction)
		assertEquals(InstallMode.Full, parameters.installMode)
		assertEquals(InstallPreapproval.NONE, parameters.preapproval)
		assertEquals(InstallConstraints.NONE, parameters.constraints)
		assertFalse(parameters.requestUpdateOwnership)
		assertEquals(PackageSource.Unspecified, parameters.packageSource)
		assertEquals(emptyMap(), parameters.v4Signatures)
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
	fun lowApiEnforcesIntentBasedInstallerType() {
		SdkInt.set(19)
		val parameters = InstallParameters.Builder(Uri.EMPTY)
			.setInstallerType(InstallerType.SESSION_BASED)
			.build()
		assertEquals(InstallerType.INTENT_BASED, parameters.installerType)
	}

	@Test
	fun lowApiRejectsMultipleApks() {
		SdkInt.set(19)
		assertFailsWith<SplitPackagesNotSupportedException> {
			InstallParameters.Builder(listOf(Uri.EMPTY, Uri.EMPTY))
		}
	}

	@Test
	fun lowApiRejectsAddApkMethods() {
		SdkInt.set(19)
		assertFailsWith<SplitPackagesNotSupportedException> {
			InstallParameters.Builder(Uri.EMPTY).addApk(Uri.EMPTY)
		}
		assertFailsWith<SplitPackagesNotSupportedException> {
			InstallParameters.Builder(Uri.EMPTY).addApks(listOf(Uri.EMPTY, Uri.EMPTY))
		}
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
		val v4Signature = Uri.parse("file:///base.apk.idsig")
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
			.setV4Signatures(mapOf(Uri.EMPTY to v4Signature))
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
		assertEquals(mapOf(Uri.EMPTY to v4Signature), parameters.v4Signatures)
	}

	@OptIn(DelicateAckpineApi::class)
	@Test
	fun pluginIsAppliedDuringBuild() {
		val parameters = InstallParameters.Builder(Uri.EMPTY)
			.setRequireUserAction(true)
			.registerPlugin(TestPlugin::class.java, TestPlugin.Parameters(""))
			.build()
		assertFalse(parameters.requireUserAction)
	}

	@OptIn(DelicateAckpineApi::class)
	@Test
	fun parameterlessPluginIsAppliedDuringBuild() {
		val parameters = InstallParameters.Builder(Uri.EMPTY)
			.setRequireUserAction(true)
			.registerPlugin(TestParameterlessPlugin::class.java)
			.build()
		assertFalse(parameters.requireUserAction)
	}

	@OptIn(DelicateAckpineApi::class)
	@Test
	fun chainedPluginIsAppliedDuringBuild() {
		val parameters = InstallParameters.Builder(Uri.EMPTY)
			.setRequireUserAction(true)
			.registerPlugin(ChainedTestPlugin::class.java)
			.build()
		val expectedPlugins = mapOf<Class<out AckpinePlugin>, AckpinePlugin.Parameters>(
			ChainedTestPlugin::class.java to AckpinePlugin.Parameters.None,
			ChainedPlugin::class.java to AckpinePlugin.Parameters.None,
			TestParameterlessPlugin::class.java to AckpinePlugin.Parameters.None
		)
		assertFalse(parameters.requireUserAction)
		assertEquals(expectedPlugins, parameters.pluginContainer.getPlugins())
	}

	@Test
	fun pluginParametersArePreserved() {
		val parameters = InstallParameters.Builder(Uri.EMPTY)
			.registerPlugin(TestPlugin::class.java, TestPlugin.Parameters("value"))
			.build()
		val expectedPlugins = mapOf<Class<out AckpinePlugin>, TestPlugin.Parameters>(
			TestPlugin::class.java to TestPlugin.Parameters("value")
		)
		assertEquals(expectedPlugins, parameters.pluginContainer.getPlugins())
	}

	@Test
	fun buildIsIdempotent() {
		val builder = InstallParameters.Builder(Uri.EMPTY)
			.registerPlugin(ChainedTestPlugin::class.java)
		val first = builder.build()
		val second = builder.build()
		assertEquals(first, second)
	}

	@Test
	fun transitivePluginObservesNormalizedInstallerType() {
		val parameters = InstallParameters.Builder(listOf(Uri.EMPTY, Uri.EMPTY))
			.registerPlugin(BackendFlipperPlugin::class.java)
			.build()
		val expectedPlugins = mapOf<Class<out AckpinePlugin>, AckpinePlugin.Parameters>(
			BackendFlipperPlugin::class.java to AckpinePlugin.Parameters.None,
			IntentBasedBackendObserverPlugin::class.java to AckpinePlugin.Parameters.None
			// observer doesn't apply TestParameterlessPlugin on session-based
		)
		assertEquals(InstallerType.SESSION_BASED, parameters.installerType)
		assertEquals(expectedPlugins, parameters.pluginContainer.getPlugins())
	}

	@Test
	fun setV4SignaturesReplacesPreviouslyAddedEntries() {
		val baseApk = Uri.parse("file:///base.apk")
		val split = Uri.parse("file:///split.apk")
		val builder = InstallParameters.Builder(listOf(baseApk, split))
			.addV4Signature(baseApk, Uri.parse("file:///stale.idsig"))
			.setV4Signatures(mapOf(split to Uri.parse("file:///split.apk.idsig")))
		assertEquals(mapOf(split to Uri.parse("file:///split.apk.idsig")), builder.build().v4Signatures)
	}

	@Test
	fun addV4SignatureAccumulatesEntries() {
		val baseApk = Uri.parse("file:///base.apk")
		val split = Uri.parse("file:///split.apk")
		val parameters = InstallParameters.Builder(listOf(baseApk, split))
			.addV4Signature(baseApk, Uri.parse("file:///base.apk.idsig"))
			.addV4Signature(split, Uri.parse("file:///split.apk.idsig"))
			.build()
		assertEquals(
			mapOf(
				baseApk to Uri.parse("file:///base.apk.idsig"),
				split to Uri.parse("file:///split.apk.idsig")
			),
			parameters.v4Signatures
		)
	}

	@Test
	fun v4SignaturesInBuiltParametersAreNotAffectedByLaterBuilderMutations() {
		val baseApk = Uri.parse("file:///base.apk")
		val builder = InstallParameters.Builder(baseApk)
			.addV4Signature(baseApk, Uri.parse("file:///base.apk.idsig"))
		val parameters = builder.build()
		builder.setV4Signatures(emptyMap())
		assertEquals(mapOf(baseApk to Uri.parse("file:///base.apk.idsig")), parameters.v4Signatures)
	}

	@Test
	fun buildFailsWhenV4SignatureIsProvidedForApkNotAddedToSession() {
		val baseApk = Uri.parse("file:///base.apk")
		val unknownApk = Uri.parse("file:///unknown.apk")
		val builder = InstallParameters.Builder(baseApk)
			.addV4Signature(unknownApk, Uri.parse("file:///unknown.apk.idsig"))
		val exception = assertFailsWith<IllegalArgumentException> { builder.build() }
		assertContains(exception.message.orEmpty(), unknownApk.toString())
	}

	@Test
	fun v4SignaturesAreTakenIntoAccountInEquality() {
		val baseApk = Uri.parse("file:///base.apk")
		val withSignature = InstallParameters.Builder(baseApk)
			.addV4Signature(baseApk, Uri.parse("file:///base.apk.idsig"))
			.build()
		val withoutSignature = InstallParameters.Builder(baseApk).build()
		assertNotEquals(withoutSignature, withSignature)
		assertNotEquals(withoutSignature.hashCode(), withSignature.hashCode())
		assertContains(withSignature.toString(), "v4Signatures=")
	}
}
