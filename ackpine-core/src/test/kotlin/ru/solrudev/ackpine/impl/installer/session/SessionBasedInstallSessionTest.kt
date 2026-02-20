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

package ru.solrudev.ackpine.impl.installer.session

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import ru.solrudev.ackpine.impl.helpers.concurrent.BinarySemaphore
import ru.solrudev.ackpine.impl.services.PackageInstallerService
import ru.solrudev.ackpine.impl.testutil.CommitAttemptsUpdate
import ru.solrudev.ackpine.impl.testutil.ImmediateExecutor
import ru.solrudev.ackpine.impl.testutil.RecordingInstallConstraintsDao
import ru.solrudev.ackpine.impl.testutil.RecordingInstallPreapprovalDao
import ru.solrudev.ackpine.impl.testutil.RecordingNativeSessionIdDao
import ru.solrudev.ackpine.impl.testutil.RecordingPackageInstallerService
import ru.solrudev.ackpine.impl.testutil.RecordingSessionDao
import ru.solrudev.ackpine.impl.testutil.RecordingSessionProgressDao
import ru.solrudev.ackpine.impl.testutil.TestSessionFailureDao
import ru.solrudev.ackpine.impl.testutil.captureProgress
import ru.solrudev.ackpine.impl.testutil.captureStates
import ru.solrudev.ackpine.impl.testutil.createAckpineFile
import ru.solrudev.ackpine.impl.testutil.deleteAckpineFiles
import ru.solrudev.ackpine.impl.testutil.idleMainThread
import ru.solrudev.ackpine.installer.InstallFailure
import ru.solrudev.ackpine.installer.parameters.InstallConstraints
import ru.solrudev.ackpine.installer.parameters.InstallConstraints.TimeoutStrategy
import ru.solrudev.ackpine.installer.parameters.InstallMode
import ru.solrudev.ackpine.installer.parameters.InstallPreapproval
import ru.solrudev.ackpine.installer.parameters.PackageSource
import ru.solrudev.ackpine.session.Progress
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.session.parameters.Confirmation
import ru.solrudev.ackpine.session.parameters.NotificationData
import java.io.File
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

@RunWith(RobolectricTestRunner::class)
class SessionBasedInstallSessionTest {

	private val context: Context = ApplicationProvider.getApplicationContext()
	private val handler = Handler(Looper.getMainLooper())
	private val dbWriteSemaphore = BinarySemaphore()

	@AfterTest
	fun tearDown() {
		context.deleteAckpineFiles()
	}

	@Test
	fun launchWritesApkToPackageInstallerSessionAndTransitionsToAwaiting() {
		val sessionId = UUID.randomUUID()
		val apkFile = context.createAckpineFile("test/$sessionId.apk") { writeText("test apk") }
		val packageInstaller = RecordingPackageInstallerService()
		val session = createSession(
			packageInstaller = packageInstaller,
			apks = listOf(apkFile.toUri()),
			id = sessionId,
			initialState = Session.State.Pending
		)
		val states = session.captureStates()

		session.launch()
		idleMainThread()

		val writtenApk = packageInstaller.session.writes["0.apk"]
		assertNotNull(writtenApk)
		assertEquals("test apk", writtenApk.toString(Charsets.UTF_8))
		assertEquals(Session.State.Awaiting, states.last())
	}

	@Test
	fun launchWithSplitApksWritesAllToSession() {
		val sessionId = UUID.randomUUID()
		val apkFiles = listOf(
			context.createAckpineFile("test/split-$sessionId-1.apk") { writeText("apk 1") },
			context.createAckpineFile("test/split-$sessionId-2.apk") { writeText("apk 2") },
			context.createAckpineFile("test/split-$sessionId-3.apk") { writeText("apk 3") }
		)
		val packageInstaller = RecordingPackageInstallerService()
		val session = createSession(
			packageInstaller = packageInstaller,
			apks = apkFiles.map(File::toUri),
			id = sessionId,
			initialState = Session.State.Pending
		)
		val states = session.captureStates()

		session.launch()
		idleMainThread()

		assertContains(states, Session.State.Awaiting)
		assertEquals(3, packageInstaller.session.writes.size)
		val writtenApk0 = packageInstaller.session.writes["0.apk"]
		val writtenApk1 = packageInstaller.session.writes["1.apk"]
		val writtenApk2 = packageInstaller.session.writes["2.apk"]
		assertNotNull(writtenApk0)
		assertNotNull(writtenApk1)
		assertNotNull(writtenApk2)
		assertEquals("apk 1", writtenApk0.toString(Charsets.UTF_8))
		assertEquals("apk 2", writtenApk1.toString(Charsets.UTF_8))
		assertEquals("apk 3", writtenApk2.toString(Charsets.UTF_8))
	}

	@Test
	fun launchWithPreapprovalRequestsUserPreapproval() {
		val sessionId = UUID.randomUUID()
		val icon = context.createAckpineFile("icon.png").toUri()
		val packageInstaller = RecordingPackageInstallerService()
		val session = createSession(
			packageInstaller = packageInstaller,
			id = sessionId,
			preapproval = InstallPreapproval("pkg", "label", "en", icon),
			initialState = Session.State.Pending
		)

		session.launch()
		idleMainThread()

		assertEquals(1, packageInstaller.session.preapprovalRequests.size)
	}

	@Test
	fun commitCommitsPackageInstallerSessionAndPersistsCommitAttempt() {
		val constraintsDao = RecordingInstallConstraintsDao()
		val sessionId = UUID.randomUUID()
		val apkFile = context.createAckpineFile("test/commit-$sessionId.apk") { writeText("apk") }
		val packageInstaller = RecordingPackageInstallerService()
		val session = createSession(
			packageInstaller = packageInstaller,
			apks = listOf(apkFile.toUri()),
			id = sessionId,
			constraintsDao = constraintsDao,
			initialState = Session.State.Pending
		)

		session.launch()
		idleMainThread()
		assertTrue(session.commit())
		idleMainThread()

		assertFalse(packageInstaller.session.commits.isEmpty())
		val expectedUpdate = CommitAttemptsUpdate(sessionId.toString(), commitAttemptsCount = 1)
		assertContains(constraintsDao.commitAttemptsUpdates, expectedUpdate)
	}

	@Test
	fun commitWithInstallConstraintsUsesCommitAfterConstraintsApi() {
		val constraints = InstallConstraints.gentleUpdate(1.seconds)
		val sessionId = UUID.randomUUID()
		val apkFile = context.createAckpineFile("test/constraints-$sessionId.apk")
		val packageInstaller = RecordingPackageInstallerService()
		val session = createSession(
			packageInstaller = packageInstaller,
			apks = listOf(apkFile.toUri()),
			id = sessionId,
			constraints = constraints,
			initialState = Session.State.Pending
		)

		session.launch()
		idleMainThread()
		assertTrue(session.commit())
		idleMainThread()

		assertEquals(1, packageInstaller.commitAfterConstraintsCalls.size)
	}

	@Test
	fun preapprovalSucceededSetsIsPreapproved() {
		val preapprovalDao = RecordingInstallPreapprovalDao()
		val session = createSession(
			preapprovalDao = preapprovalDao,
			preapproval = InstallPreapproval("pkg", "label", "en"),
			initialState = Session.State.Pending
		)

		session.onPreapprovalSucceeded()
		idleMainThread()

		assertContains(preapprovalDao.preapprovedSessions, session.id.toString())
	}

	@Test
	fun preapprovalFailureCompletesFailedWithoutFallback() {
		val session = createSession(
			preapproval = InstallPreapproval("pkg", "label", "en"),
			initialState = Session.State.Pending
		)
		val states = session.captureStates()

		val expectedFailure = InstallFailure.Generic("fail")
		session.onPreapprovalFailed(
			PackageInstallerStatus.INSTALL_FAILED_PRE_APPROVAL_NOT_AVAILABLE,
			expectedFailure
		)
		idleMainThread()

		val state = states.last()
		assertIs<Session.State.Failed<InstallFailure>>(state)
		assertEquals(expectedFailure, state.failure)
	}

	@Test
	fun preapprovalFailedWithFallbackRecreatesNativeSessionAndLaunchesSession() {
		val nativeSessionIdDao = RecordingNativeSessionIdDao()
		val packageInstaller = RecordingPackageInstallerService()
		val sessionId = UUID.randomUUID()
		val apkFile = context.createAckpineFile("test/fake.apk")
		val session = createSession(
			id = sessionId,
			apks = listOf(apkFile.toUri()),
			packageInstaller = packageInstaller,
			nativeSessionIdDao = nativeSessionIdDao,
			preapproval = InstallPreapproval("pkg", "label", "en") {
				fallbackToOnDemandApproval = true
			},
			initialState = Session.State.Pending
		)
		val states = session.captureStates()

		assertTrue(packageInstaller.session.writes.isEmpty())

		session.onPreapprovalFailed(
			PackageInstallerStatus.INSTALL_FAILED_PRE_APPROVAL_NOT_AVAILABLE,
			InstallFailure.Generic("preapproval not available")
		)
		idleMainThread()

		assertContains(nativeSessionIdDao.removed, sessionId.toString())
		assertFalse(packageInstaller.session.writes.isEmpty())
		assertEquals(Session.State.Awaiting, states.last())
	}

	@Test
	fun preapprovalFailedWithNullStatusCompletesWithFailure() {
		val session = createSession(
			preapproval = InstallPreapproval("pkg", "label", "en") {
				fallbackToOnDemandApproval = true
			},
			initialState = Session.State.Pending
		)
		val states = session.captureStates()

		val expectedFailure = InstallFailure.Generic("unknown error")
		session.onPreapprovalFailed(status = null, expectedFailure)
		idleMainThread()

		val state = states.last()
		assertIs<Session.State.Failed<InstallFailure>>(state)
		assertEquals(expectedFailure, state.failure)
	}

	@Test
	fun timeoutRetryMovesSessionBackToAwaiting() {
		val constraints = InstallConstraints(1.seconds) {
			timeoutStrategy = TimeoutStrategy.Retry(2)
		}
		val session = createSession(
			constraints = constraints,
			initialState = Session.State.Committed,
			commitAttemptsCount = 1
		)
		val states = session.captureStates()

		session.complete(Session.State.Failed(InstallFailure.Timeout("timeout")))
		idleMainThread()

		assertEquals(Session.State.Awaiting, states.last())
	}

	@Test
	fun timeoutRetryExhaustedCompletesWithFailure() {
		val constraints = InstallConstraints(1.seconds) {
			timeoutStrategy = TimeoutStrategy.Retry(2)
		}
		val session = createSession(
			constraints = constraints,
			initialState = Session.State.Committed,
			commitAttemptsCount = 3 // Exceeded retries
		)
		val states = session.captureStates()

		val expectedFailure = InstallFailure.Timeout("timeout")
		session.complete(Session.State.Failed(expectedFailure))
		idleMainThread()

		val state = states.last()
		assertIs<Session.State.Failed<InstallFailure>>(state)
		assertEquals(expectedFailure, state.failure)
	}

	@Test
	fun commitEagerlyFirstAttemptMovesSessionToAwaiting() {
		val constraints = InstallConstraints(1.seconds) {
			timeoutStrategy = TimeoutStrategy.CommitEagerly
		}
		val session = createSession(
			constraints = constraints,
			initialState = Session.State.Committed,
			commitAttemptsCount = 1
		)
		val states = session.captureStates()

		session.complete(Session.State.Failed(InstallFailure.Timeout("timeout")))
		idleMainThread()

		assertEquals(Session.State.Awaiting, states.last())
	}

	@Test
	fun commitEagerlySecondAttemptCompletesWithFailure() {
		val constraints = InstallConstraints(1.seconds) {
			timeoutStrategy = TimeoutStrategy.CommitEagerly
		}
		val session = createSession(
			constraints = constraints,
			initialState = Session.State.Committed,
			commitAttemptsCount = 2 // Second attempt
		)
		val states = session.captureStates()

		val expectedFailure = InstallFailure.Generic("fail")
		session.complete(Session.State.Failed(expectedFailure))
		idleMainThread()

		val state = states.last()
		assertIs<Session.State.Failed<InstallFailure>>(state)
		assertEquals(expectedFailure, state.failure)
	}

	@Test
	fun timeoutFailStrategyCompletesImmediately() {
		val constraints = InstallConstraints(1.seconds) {
			timeoutStrategy = TimeoutStrategy.Fail
		}
		val session = createSession(
			constraints = constraints,
			initialState = Session.State.Committed,
			commitAttemptsCount = 1
		)
		val states = session.captureStates()

		val expectedFailure = InstallFailure.Timeout("timeout")
		session.complete(Session.State.Failed(expectedFailure))
		idleMainThread()

		val state = states.last()
		assertIs<Session.State.Failed<InstallFailure>>(state)
		assertEquals(expectedFailure, state.failure)
	}

	@Test
	fun nonTimeoutFailureCompletesNormally() {
		val constraints = InstallConstraints(1.seconds) {
			timeoutStrategy = TimeoutStrategy.Retry(5)
		}
		val session = createSession(
			constraints = constraints,
			initialState = Session.State.Committed,
			commitAttemptsCount = 1
		)
		val states = session.captureStates()

		val expectedFailure = InstallFailure.Generic("some error")
		session.complete(Session.State.Failed(expectedFailure))
		idleMainThread()

		val state = states.last()
		assertIs<Session.State.Failed<InstallFailure>>(state)
		assertEquals(expectedFailure, state.failure)
	}

	@Test
	fun successCompletesNormallyWithConstraints() {
		val constraints = InstallConstraints(1.seconds) {
			timeoutStrategy = TimeoutStrategy.Retry(5)
		}
		val session = createSession(
			constraints = constraints,
			initialState = Session.State.Committed,
			commitAttemptsCount = 1
		)
		val states = session.captureStates()

		session.complete(Session.State.Succeeded)
		idleMainThread()

		assertEquals(Session.State.Succeeded, states.last())
	}

	@Test
	fun sessionWithNativeSessionIdRegistersCallback() {
		val packageInstaller = RecordingPackageInstallerService()
		createSession(
			packageInstaller = packageInstaller,
			initialState = Session.State.Active,
			nativeSessionId = 123
		)
		idleMainThread()

		assertEquals(1, packageInstaller.registeredCallbacks.size)
	}

	@Test
	fun sessionCallbackProgressUpdatesSessionProgress() {
		val packageInstaller = RecordingPackageInstallerService()
		val session = createSession(
			packageInstaller = packageInstaller,
			initialState = Session.State.Active,
			nativeSessionId = 123
		)
		val progressEvents = session.captureProgress()

		val callback = packageInstaller.registeredCallbacks.single()
		callback.onProgressChanged(999, 0.6f) // wrong native session ID
		callback.onProgressChanged(123, 0.5f)
		idleMainThread()

		val expectedProgressEvents = listOf(Progress(), Progress(50, 100))
		assertEquals(expectedProgressEvents, progressEvents)
	}

	@Test
	fun cancelAbandonsSessionAndUnregistersCallback() {
		val packageInstaller = RecordingPackageInstallerService()
		val session = createSession(
			packageInstaller = packageInstaller,
			initialState = Session.State.Active,
			nativeSessionId = 123
		)
		idleMainThread()

		session.cancel()
		idleMainThread()

		assertContains(packageInstaller.abandonedSessions, 123)
		assertEquals(1, packageInstaller.unregisteredCallbacks.size)
	}

	private fun createSession(
		packageInstaller: PackageInstallerService = RecordingPackageInstallerService(),
		apks: List<Uri> = listOf(Uri.EMPTY),
		id: UUID = UUID.randomUUID(),
		preapproval: InstallPreapproval = InstallPreapproval.NONE,
		constraints: InstallConstraints = InstallConstraints.NONE,
		initialState: Session.State<InstallFailure> = Session.State.Pending,
		commitAttemptsCount: Int = 0,
		installMode: InstallMode = InstallMode.Full,
		packageSource: PackageSource = PackageSource.Unspecified,
		nativeSessionId: Int = -1,
		isPreapproved: Boolean = false,
		initialProgress: Progress = Progress(),
		nativeSessionIdDao: RecordingNativeSessionIdDao = RecordingNativeSessionIdDao(),
		preapprovalDao: RecordingInstallPreapprovalDao = RecordingInstallPreapprovalDao(),
		constraintsDao: RecordingInstallConstraintsDao = RecordingInstallConstraintsDao()
	) = SessionBasedInstallSession(
		context = context,
		packageInstaller = packageInstaller,
		apks = apks,
		id = id,
		initialState = initialState,
		initialProgress = initialProgress,
		confirmation = Confirmation.DEFERRED,
		notificationData = NotificationData.DEFAULT,
		requireUserAction = true,
		installMode = installMode,
		preapproval = preapproval,
		constraints = constraints,
		requestUpdateOwnership = false,
		packageSource = packageSource,
		sessionDao = RecordingSessionDao(),
		sessionFailureDao = TestSessionFailureDao(),
		sessionProgressDao = RecordingSessionProgressDao(),
		nativeSessionIdDao = nativeSessionIdDao,
		installPreapprovalDao = preapprovalDao,
		installConstraintsDao = constraintsDao,
		executor = ImmediateExecutor,
		handler = handler,
		sessionCallbackHandler = handler,
		nativeSessionId = nativeSessionId,
		notificationId = 1,
		commitAttemptsCount = commitAttemptsCount,
		isPreapproved = isPreapproved,
		dbWriteSemaphore = dbWriteSemaphore
	)
}