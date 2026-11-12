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

package ru.solrudev.ackpine.impl.uninstaller.session

import android.content.Context
import android.content.IntentSender
import android.content.pm.VersionedPackage
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.solrudev.ackpine.impl.helpers.concurrent.BinarySemaphore
import ru.solrudev.ackpine.impl.logging.AckpineLoggerProvider
import ru.solrudev.ackpine.impl.services.PackageInstallerService
import ru.solrudev.ackpine.impl.testutil.ImmediateExecutor
import ru.solrudev.ackpine.impl.testutil.RecordingAckpineLogger
import ru.solrudev.ackpine.impl.testutil.RecordingPackageInstallerService
import ru.solrudev.ackpine.impl.testutil.RecordingSessionDao
import ru.solrudev.ackpine.impl.testutil.TestSessionFailureDao
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.session.parameters.Confirmation
import ru.solrudev.ackpine.session.parameters.NotificationData
import ru.solrudev.ackpine.uninstaller.UninstallFailure
import ru.solrudev.ackpine.uninstaller.parameters.PackageVersionCode
import ru.solrudev.ackpine.uninstaller.parameters.VersionedUninstallPolicy
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class PackageInstallerBasedUninstallSessionTest {

	private val context: Context = ApplicationProvider.getApplicationContext()

	@Test
	fun commitCallsPackageInstallerUninstall() {
		val packageInstaller = RecordingPackageInstallerService()
		val sessionId = UUID.randomUUID()
		val session = createSession(
			packageInstaller = packageInstaller,
			id = sessionId
		)

		assertTrue(session.commit())

		val call = packageInstaller.uninstallCalls.single()
		assertEquals("com.example.app", call.packageName)
		assertEquals(sessionId, call.ackpineSessionId)
	}

	@Test
	@Config(sdk = [Build.VERSION_CODES.O])
	fun exactVersionWithinIntRangeIsDispatchedWithoutLosingValue() {
		val service = RecordingPackageInstallerService()
		val version = PackageVersionCode(Int.MAX_VALUE)
		val session = createSession(service, packageVersionCode = version)
		assertTrue(session.commit())
		assertEquals(version, service.uninstallCalls.single().packageVersionCode)
	}

	@Test
	fun exactLongVersionIsDispatchedWithoutNarrowing() {
		val service = RecordingPackageInstallerService()
		val version = PackageVersionCode(Long.MAX_VALUE)
		val session = createSession(service, packageVersionCode = version)
		assertTrue(session.commit())
		assertEquals(version, service.uninstallCalls.single().packageVersionCode)
	}

	@Test
	@Config(sdk = [Build.VERSION_CODES.M])
	fun restoredStrictTargetBelowApi26FailsBeforeDispatch() {
		val service = RecordingPackageInstallerService()
		val version = PackageVersionCode(42)
		val failureDao = TestSessionFailureDao<UninstallFailure>()
		val session = createSession(service, packageVersionCode = version, failureDao = failureDao)
		assertTrue(session.commit())
		assertTrue(session.isCompleted)
		assertIs<UninstallFailure.Unsupported>(failureDao.getFailure(session.id.toString()))
		assertTrue(service.uninstallCalls.isEmpty())
	}

	@Test
	@Config(sdk = [Build.VERSION_CODES.O])
	fun restoredStrictTargetExceedingIntRangeBelowApi28FailsBeforeDispatch() {
		val service = RecordingPackageInstallerService()
		val version = PackageVersionCode(Int.MAX_VALUE.toLong() + 1)
		val failureDao = TestSessionFailureDao<UninstallFailure>()
		val session = createSession(service, packageVersionCode = version, failureDao = failureDao)
		assertTrue(session.commit())
		assertTrue(session.isCompleted)
		assertIs<UninstallFailure.Unsupported>(failureDao.getFailure(session.id.toString()))
		assertTrue(service.uninstallCalls.isEmpty())
	}

	@Test
	@Config(sdk = [Build.VERSION_CODES.M])
	fun unsafeTargetBelowApi26UsesNameOnly() {
		val service = RecordingPackageInstallerService()
		val version = PackageVersionCode(42)
		val session = createSession(
			service,
			packageVersionCode = version,
			policy = VersionedUninstallPolicy.ALLOW_NAME_ONLY
		)
		assertTrue(session.commit())
		assertEquals(PackageVersionCode.HIGHEST, service.uninstallCalls.single().packageVersionCode)
	}

	@Test
	@Config(sdk = [Build.VERSION_CODES.O])
	fun unsafeTargetExceedingIntRangeBelowApi28UsesNameOnly() {
		val service = RecordingPackageInstallerService()
		val version = PackageVersionCode(Int.MAX_VALUE.toLong() + 1)
		val session = createSession(
			service,
			packageVersionCode = version,
			policy = VersionedUninstallPolicy.ALLOW_NAME_ONLY
		)
		assertTrue(session.commit())
		assertEquals(PackageVersionCode.HIGHEST, service.uninstallCalls.single().packageVersionCode)
	}

	@Test
	fun inheritedVersionedDefaultRefusesStrictTargetWithoutNameOnlyDispatch() {
		val recording = RecordingPackageInstallerService()
		val failureDao = TestSessionFailureDao<UninstallFailure>()
		val session = createSession(
			nameOnlyService(recording),
			packageVersionCode = PackageVersionCode(42),
			failureDao = failureDao
		)
		assertTrue(session.commit())
		assertTrue(session.isCompleted)
		assertIs<UninstallFailure.Unsupported>(failureDao.getFailure(session.id.toString()))
		assertTrue(recording.uninstallCalls.isEmpty())
	}

	@Test
	fun inheritedVersionedDefaultAllowsExplicitNameOnlyFallback() {
		val recording = RecordingPackageInstallerService()
		val session = createSession(
			nameOnlyService(recording),
			packageVersionCode = PackageVersionCode(42),
			policy = VersionedUninstallPolicy.ALLOW_NAME_ONLY
		)
		assertTrue(session.commit())
		assertEquals(PackageVersionCode.HIGHEST, recording.uninstallCalls.single().packageVersionCode)
	}

	@Test
	fun arbitraryBackendFailureDoesNotTriggerUnsafeFallback() {
		val recording = RecordingPackageInstallerService()
		val service = object : PackageInstallerService by recording {
			override fun uninstall(
				versionedPackage: VersionedPackage,
				statusReceiver: IntentSender,
				ackpineSessionId: UUID
			) = error("Backend failed")
		}
		val failureDao = TestSessionFailureDao<UninstallFailure>()
		val session = createSession(
			service,
			packageVersionCode = PackageVersionCode(42),
			policy = VersionedUninstallPolicy.ALLOW_NAME_ONLY,
			failureDao = failureDao
		)
		assertTrue(session.commit())
		assertTrue(session.isCompleted)
		assertIs<UninstallFailure.Exceptional>(failureDao.getFailure(session.id.toString()))
		assertTrue(recording.uninstallCalls.isEmpty())
	}

	private fun nameOnlyService(
		recording: RecordingPackageInstallerService
	) = object : PackageInstallerService by recording {
		override fun uninstall(
			versionedPackage: VersionedPackage,
			statusReceiver: IntentSender,
			ackpineSessionId: UUID
		) = super.uninstall(versionedPackage, statusReceiver, ackpineSessionId)
	}

	private fun createSession(
		packageInstaller: PackageInstallerService,
		id: UUID = UUID.randomUUID(),
		packageVersionCode: PackageVersionCode = PackageVersionCode.HIGHEST,
		policy: VersionedUninstallPolicy = VersionedUninstallPolicy.STRICT,
		initialState: Session.State<UninstallFailure> = Session.State.Awaiting,
		logger: RecordingAckpineLogger? = null,
		failureDao: TestSessionFailureDao<UninstallFailure> = TestSessionFailureDao()
	) = PackageInstallerBasedUninstallSession(
		loggerProvider = AckpineLoggerProvider("PackageInstallerBasedUninstallSession") { logger },
		context = context,
		packageInstallerService = lazyOf(packageInstaller),
		packageName = "com.example.app",
		packageVersionCode = packageVersionCode,
		versionedUninstallPolicy = policy,
		id = id,
		initialState = initialState,
		confirmation = Confirmation.DEFERRED,
		notificationData = NotificationData.DEFAULT,
		sessionDao = RecordingSessionDao(),
		sessionFailureDao = failureDao,
		executor = ImmediateExecutor,
		handler = Handler(Looper.getMainLooper()),
		notificationId = 1,
		dbWriteSemaphore = BinarySemaphore()
	)
}