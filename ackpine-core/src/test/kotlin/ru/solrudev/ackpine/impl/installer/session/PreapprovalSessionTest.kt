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
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import ru.solrudev.ackpine.impl.testutil.RecordingInstallPreapprovalDao
import ru.solrudev.ackpine.impl.testutil.RecordingNativeSessionIdDao
import ru.solrudev.ackpine.impl.testutil.RecordingPackageInstallerService
import ru.solrudev.ackpine.impl.testutil.captureStates
import ru.solrudev.ackpine.impl.testutil.createAckpineFile
import ru.solrudev.ackpine.impl.testutil.deleteAckpineFiles
import ru.solrudev.ackpine.impl.testutil.idleMainThread
import ru.solrudev.ackpine.installer.InstallFailure
import ru.solrudev.ackpine.installer.parameters.InstallPreapproval
import ru.solrudev.ackpine.session.Session
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class PreapprovalSessionTest {

	private val context: Context = ApplicationProvider.getApplicationContext()

	@AfterTest
	fun tearDown() {
		context.deleteAckpineFiles()
	}

	@Test
	fun launchWithPreapprovalRequestsUserPreapproval() {
		val sessionId = UUID.randomUUID()
		val packageInstaller = RecordingPackageInstallerService()
		val preapprovalDao = RecordingInstallPreapprovalDao()
		val session = createSessionBasedSession(
			packageInstaller = packageInstaller,
			preapprovalDao = preapprovalDao,
			id = sessionId,
			preapproval = InstallPreapproval("pkg", "label", "en"),
			initialState = Session.State.Pending
		)

		session.launch()
		idleMainThread()

		assertEquals(1, packageInstaller.session.preapprovalRequests.size)
		assertFalse(sessionId.toString() in preapprovalDao.activatingSessions)
		assertContains(preapprovalDao.activeSessions, sessionId.toString())
	}

	@Test
	fun preapprovalSuccessMarksPreapprovedAndContinuesPreparing() {
		val preapprovalDao = RecordingInstallPreapprovalDao()
		val apkFile = context.createAckpineFile("test/preapproval-success.apk")
		val session = createSessionBasedSession(
			apks = listOf(apkFile.toUri()),
			preapprovalDao = preapprovalDao,
			preapproval = InstallPreapproval("pkg", "label", "en"),
			initialState = Session.State.Pending
		)
		val states = session.captureStates()

		session.launch()
		idleMainThread()
		session.onPreapprovalSucceeded()
		idleMainThread()

		assertContains(preapprovalDao.preapprovedSessions, session.id.toString())
		assertEquals(Session.State.Awaiting, states.last())
	}

	@Test
	fun duplicatePreapprovalSuccessIsIgnored() {
		val preapprovalDao = RecordingInstallPreapprovalDao()
		val apkFile = context.createAckpineFile("test/preapproval-duplicate.apk")
		val session = createSessionBasedSession(
			apks = listOf(apkFile.toUri()),
			preapprovalDao = preapprovalDao,
			preapproval = InstallPreapproval("pkg", "label", "en"),
			initialState = Session.State.Pending
		)

		session.launch()
		idleMainThread()
		session.onPreapprovalSucceeded()
		idleMainThread()
		session.onPreapprovalSucceeded()
		idleMainThread()

		assertEquals(1, preapprovalDao.consumeCalls.count { it.result == 1 && it.isPreapproved })
		assertEquals(1, preapprovalDao.preapprovedSessions.count { it == session.id.toString() })
	}

	@Test
	fun preapprovalIsNoLongerActiveAfterSuccess() {
		val preapprovalDao = RecordingInstallPreapprovalDao()
		val apkFile = context.createAckpineFile("test/preapproval-late-start.apk")
		val packageInstaller = RecordingPackageInstallerService()
		val session = createSessionBasedSession(
			apks = listOf(apkFile.toUri()),
			packageInstaller = packageInstaller,
			preapprovalDao = preapprovalDao,
			preapproval = InstallPreapproval("pkg", "label", "en"),
			initialState = Session.State.Pending
		)
		val states = session.captureStates()

		session.launch()
		idleMainThread()
		session.onPreapprovalSucceeded()
		idleMainThread()

		assertFalse(session.isPreapprovalActive())
		assertEquals(Session.State.Awaiting, states.last())
		assertEquals(1, packageInstaller.session.preapprovalRequests.size)
	}

	@Test
	fun restoredActivePreapprovalIsReportedActive() {
		val session = createSessionBasedSession(
			preapproval = InstallPreapproval("pkg", "label", "en"),
			preapprovalState = PreapprovalLifecycle.State.ACTIVE,
			initialState = Session.State.Active
		)
		assertTrue(session.isPreapprovalActive())
	}

	@Test
	fun restoredActivatingPreapprovalIsReportedActive() {
		val session = createSessionBasedSession(
			preapproval = InstallPreapproval("pkg", "label", "en"),
			preapprovalState = PreapprovalLifecycle.State.ACTIVATING,
			initialState = Session.State.Active
		)
		assertTrue(session.isPreapprovalActive())
	}

	@Test
	fun restoredInactivePreapprovalIsReportedInactive() {
		val session = createSessionBasedSession(
			preapproval = InstallPreapproval("pkg", "label", "en"),
			preapprovalState = PreapprovalLifecycle.State.IDLE,
			initialState = Session.State.Active
		)
		assertFalse(session.isPreapprovalActive())
	}

	@Test
	fun restoredPreapprovedSessionRequestsPreapprovalAgainWhenNativeSessionIsMissing() {
		val sessionId = UUID.randomUUID()
		val packageInstaller = RecordingPackageInstallerService()
		val preapprovalDao = RecordingInstallPreapprovalDao().apply {
			setActivating(sessionId.toString())
			setActive(sessionId.toString())
			consumeActive(sessionId.toString(), isPreapproved = true)
		}
		val session = createSessionBasedSession(
			id = sessionId,
			packageInstaller = packageInstaller,
			preapprovalDao = preapprovalDao,
			preapproval = InstallPreapproval("pkg", "label", "en"),
			initialState = Session.State.Pending,
			nativeSessionId = 42,
			preapprovalState = PreapprovalLifecycle.State.PREAPPROVED
		)

		session.launch()
		idleMainThread()

		assertEquals(1, packageInstaller.createdSessions.size)
		assertEquals(1, packageInstaller.session.preapprovalRequests.size)
		assertFalse(sessionId.toString() in preapprovalDao.preapprovedSessions)
		assertContains(preapprovalDao.activeSessions, sessionId.toString())
	}

	@Test
	fun preapprovalFailureCompletesFailedWithoutFallback() {
		val session = createSessionBasedSession(
			preapproval = InstallPreapproval("pkg", "label", "en"),
			initialState = Session.State.Pending
		)
		val states = session.captureStates()

		session.launch()
		idleMainThread()
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
	fun preapprovalFailedWithFallbackReusesNativeSessionAndLaunchesSession() {
		val nativeSessionIdDao = RecordingNativeSessionIdDao()
		val packageInstaller = RecordingPackageInstallerService()
		val sessionId = UUID.randomUUID()
		val apkFile = context.createAckpineFile("test/preapproval-fallback-reuse.apk")
		val session = createSessionBasedSession(
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

		session.launch()
		idleMainThread()
		val nativeSessionId = assertNotNull(nativeSessionIdDao.nativeSessionIds[sessionId.toString()])

		session.onPreapprovalFailed(
			PackageInstallerStatus.INSTALL_FAILED_PRE_APPROVAL_NOT_AVAILABLE,
			InstallFailure.Generic("preapproval not available")
		)
		idleMainThread()

		assertEquals(1, packageInstaller.createdSessions.size)
		assertEquals(nativeSessionId, nativeSessionIdDao.nativeSessionIds[sessionId.toString()])
		assertEquals(Session.State.Awaiting, states.last())
	}

	@Test
	fun preapprovalFailedWithFallbackRecreatesSessionWhenNativeSessionIsMissing() {
		val nativeSessionIdDao = RecordingNativeSessionIdDao()
		val packageInstaller = RecordingPackageInstallerService()
		val sessionId = UUID.randomUUID()
		val apkFile = context.createAckpineFile("test/preapproval-fallback-missing.apk")
		val session = createSessionBasedSession(
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

		session.launch()
		idleMainThread()
		val originalSessionId = assertNotNull(nativeSessionIdDao.nativeSessionIds[sessionId.toString()])
		packageInstaller.removeCreatedSessionId(originalSessionId)

		session.onPreapprovalFailed(
			PackageInstallerStatus.INSTALL_FAILED_PRE_APPROVAL_NOT_AVAILABLE,
			InstallFailure.Generic("preapproval not available")
		)
		idleMainThread()

		val recreatedSessionId = assertNotNull(nativeSessionIdDao.nativeSessionIds[sessionId.toString()])
		assertNotEquals(originalSessionId, recreatedSessionId)
		assertEquals(2, packageInstaller.createdSessions.size)
		assertEquals(Session.State.Awaiting, states.last())
	}

	@OptIn(ExperimentalCoroutinesApi::class)
	@Test
	fun preapprovalSuccessCancelledAfterConsumeDoesNotContinuePreparing() = runTest {
		lateinit var session: SessionBasedInstallSession
		val preapprovalDao = RecordingInstallPreapprovalDao(onPreapprovalConsumed = { session.cancel() })
		val packageInstaller = RecordingPackageInstallerService()
		val executor = StandardTestDispatcher(testScheduler).asExecutor()
		val apkFile = context.createAckpineFile("test/preapproval-cancelled.apk")
		session = createSessionBasedSession(
			apks = listOf(apkFile.toUri()),
			packageInstaller = packageInstaller,
			preapprovalDao = preapprovalDao,
			preapproval = InstallPreapproval("pkg", "label", "en"),
			initialState = Session.State.Pending,
			executor = executor
		)
		val states = session.captureStates()

		session.launch()
		advanceUntilIdle()
		assertFalse(packageInstaller.session.preapprovalRequests.isEmpty())

		session.onPreapprovalSucceeded()
		advanceUntilIdle()
		idleMainThread()

		assertEquals(Session.State.Cancelled, states.last())
		assertTrue(packageInstaller.session.writes.isEmpty())
	}

	@Test
	fun preapprovalFailedWithNullStatusCompletesWithFailure() {
		val session = createSessionBasedSession(
			preapproval = InstallPreapproval("pkg", "label", "en") {
				fallbackToOnDemandApproval = true
			},
			initialState = Session.State.Pending
		)
		val states = session.captureStates()

		val expectedFailure = InstallFailure.Generic("unknown error")
		session.launch()
		idleMainThread()
		session.onPreapprovalFailed(status = null, expectedFailure)
		idleMainThread()

		val state = states.last()
		assertIs<Session.State.Failed<InstallFailure>>(state)
		assertEquals(expectedFailure, state.failure)
	}
}