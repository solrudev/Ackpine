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

package ru.solrudev.ackpine.impl.session

import android.app.NotificationManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import androidx.test.core.app.ApplicationProvider
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import ru.solrudev.ackpine.DisposableSubscriptionContainer
import ru.solrudev.ackpine.DummyDisposableSubscription
import ru.solrudev.ackpine.impl.database.model.SessionEntity
import ru.solrudev.ackpine.impl.helpers.concurrent.BinarySemaphore
import ru.solrudev.ackpine.impl.testutil.ImmediateExecutor
import ru.solrudev.ackpine.impl.testutil.RecordingSessionDao
import ru.solrudev.ackpine.impl.testutil.SessionStateUpdate
import ru.solrudev.ackpine.impl.testutil.TestFailure
import ru.solrudev.ackpine.impl.testutil.TestSessionFailureDao
import ru.solrudev.ackpine.impl.testutil.captureStates
import ru.solrudev.ackpine.impl.testutil.idleMainThread
import ru.solrudev.ackpine.installer.InstallFailure
import ru.solrudev.ackpine.session.Failure
import ru.solrudev.ackpine.session.Session
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class AbstractSessionTest {

	private val context: Context = ApplicationProvider.getApplicationContext()
	private val handler = Handler(Looper.getMainLooper())
	private val dbWriteSemaphore = BinarySemaphore()

	@Test
	fun sessionIdPropertyReturnsCorrectId() {
		val expectedId = UUID.randomUUID()
		val session = TestSession(id = expectedId, initialState = Session.State.Pending)
		assertEquals(expectedId, session.id)
	}

	@Test
	fun stateFlagsReflectCurrentState() {
		val failedState = Session.State.Failed(TestFailure.Aborted(""))
		val stateFlags = listOf(
			SessionStateFlags(Session.State.Pending, isActive = false, isCancelled = false, isCompleted = false),
			SessionStateFlags(Session.State.Active, isActive = true, isCancelled = false, isCompleted = false),
			SessionStateFlags(Session.State.Awaiting, isActive = true, isCancelled = false, isCompleted = false),
			SessionStateFlags(Session.State.Committed, isActive = true, isCancelled = false, isCompleted = false),
			SessionStateFlags(Session.State.Succeeded, isActive = false, isCancelled = false, isCompleted = true),
			SessionStateFlags(Session.State.Cancelled, isActive = false, isCancelled = true, isCompleted = false),
			SessionStateFlags(failedState, isActive = false, isCancelled = false, isCompleted = true),
		)
		for (flags in stateFlags) {
			val session = TestSession(initialState = flags.state)
			assertEquals(flags.isActive, session.isActive, "isActive mismatch for ${flags.state}")
			assertEquals(flags.isCancelled, session.isCancelled, "isCancelled mismatch for ${flags.state}")
			assertEquals(flags.isCompleted, session.isCompleted, "isCompleted mismatch for ${flags.state}")
		}
	}

	@Test
	fun stateListenerReceivesInitialState() {
		val session = TestSession(initialState = Session.State.Pending)
		val states = session.captureStates()
		assertEquals(Session.State.Pending, states.first())
	}

	@Test
	fun addDuplicateStateListenerReturnsDummy() {
		val session = TestSession(initialState = Session.State.Pending)
		val container = DisposableSubscriptionContainer()
		val listener = Session.StateListener<TestFailure> { _, _ -> }

		val first = session.addStateListener(container, listener)
		val dummy = session.addStateListener(container, listener)

		assertNotEquals(DummyDisposableSubscription, first)
		assertEquals(DummyDisposableSubscription, dummy)
	}

	@Test
	fun disposeStateSubscriptionRemovesListener() {
		val session = TestSession(initialState = Session.State.Pending)
		val states = mutableListOf<Session.State<TestFailure>>()
		val container = DisposableSubscriptionContainer()
		val listener = Session.StateListener { _, state -> states += state }

		val subscription = session.addStateListener(container, listener)
		idleMainThread()
		subscription.dispose()

		session.launch()
		idleMainThread()

		// Only initial notification, no Active/Awaiting notifications
		assertEquals(listOf<Session.State<*>>(Session.State.Pending), states)
		assertTrue(subscription.isDisposed)
	}

	@Test
	fun multipleListenersAllNotified() {
		val session = TestSession(initialState = Session.State.Pending)
		val states1 = session.captureStates()
		val states2 = session.captureStates()
		idleMainThread()

		session.launch()
		idleMainThread()

		assertEquals(states1, states2)
		val expectedStates = listOf(
			Session.State.Pending,
			Session.State.Active,
			Session.State.Awaiting
		)
		assertEquals(expectedStates, states1)
	}

	@Test
	fun launchStateTransitions() = testLaunch(
		initialState = Session.State.Pending,
		expectedStates = listOf(
			Session.State.Pending,
			Session.State.Active,
			Session.State.Awaiting
		),
		expectedPersistedStates = listOf(
			SessionEntity.State.ACTIVE,
			SessionEntity.State.AWAITING
		)
	)

	@Test
	fun launchFromActiveIsPermitted() = testLaunch(
		initialState = Session.State.Active,
		expectedStates = listOf(
			Session.State.Active,
			Session.State.Awaiting
		),
		expectedPersistedStates = listOf(SessionEntity.State.AWAITING)
	)

	private fun testLaunch(
		initialState: Session.State<TestFailure>,
		expectedStates: List<Session.State<TestFailure>>,
		expectedPersistedStates: List<SessionEntity.State>
	) {
		val sessionDao = RecordingSessionDao()
		val session = TestSession(
			sessionDao = sessionDao,
			initialState = initialState
		)
		val states = session.captureStates()

		assertTrue(session.launch())
		assertFalse(session.launch())
		idleMainThread()

		val expectedStateUpdates = expectedPersistedStates.map { state ->
			SessionStateUpdate(session.id.toString(), state)
		}
		assertEquals(expectedStates, states)
		assertEquals(expectedStateUpdates, sessionDao.stateUpdates)
		assertFalse(sessionDao.lastLaunchUpdates.isEmpty())
		assertEquals(1, session.prepareCalls)
		assertTrue(session.isActive)
	}

	@Test
	fun launchFromWrongStateReturnsFalse() {
		for (state in listOf(
			Session.State.Awaiting,
			Session.State.Committed,
			Session.State.Cancelled,
			Session.State.Succeeded,
			Session.State.Failed(TestFailure.Aborted(""))
		)) {
			val session = TestSession(initialState = state)
			assertFalse(session.launch())
		}
	}

	@Test
	fun commitStateTransitions() = testCommit(
		initialState = Session.State.Awaiting,
		expectedStates = listOf(
			Session.State.Awaiting,
			Session.State.Committed
		),
		expectedPersistedStates = listOf(SessionEntity.State.COMMITTED),
		committedCalls = 1
	)

	@Test
	fun commitFromCommittedIsPermitted() = testCommit(
		initialState = Session.State.Committed,
		expectedStates = listOf(Session.State.Committed),
		expectedPersistedStates = emptyList(),
		committedCalls = 0
	)

	private fun testCommit(
		initialState: Session.State<TestFailure>,
		expectedStates: List<Session.State<TestFailure>>,
		expectedPersistedStates: List<SessionEntity.State>,
		committedCalls: Int
	) {
		val sessionDao = RecordingSessionDao()
		val session = TestSession(
			sessionDao = sessionDao,
			initialState = initialState
		)
		val states = session.captureStates()

		assertTrue(session.commit())
		assertFalse(session.commit())
		idleMainThread()

		val expectedStateUpdates = expectedPersistedStates.map { state ->
			SessionStateUpdate(session.id.toString(), state)
		}
		assertEquals(expectedStates, states)
		assertEquals(expectedStateUpdates, sessionDao.stateUpdates)
		assertEquals(1, session.confirmationCalls)
		assertEquals(committedCalls, session.committedCalls)
		assertFalse(sessionDao.lastCommitUpdates.isEmpty())
	}

	@Test
	fun commitFromWrongStateReturnsFalse() {
		for (state in listOf(
			Session.State.Pending,
			Session.State.Active,
			Session.State.Cancelled,
			Session.State.Succeeded,
			Session.State.Failed(TestFailure.Aborted(""))
		)) {
			val session = TestSession(initialState = state)
			assertFalse(session.commit())
		}
	}

	@Test
	fun cancelFromNonTerminalStateMarksCancelled() {
		for (state in listOf(
			Session.State.Pending,
			Session.State.Active,
			Session.State.Awaiting,
			Session.State.Committed
		)) {
			val session = TestSession(initialState = state)
			val states = session.captureStates()

			session.cancel()
			idleMainThread()

			assertEquals(Session.State.Cancelled, states.last())
		}
	}

	@Test
	fun cancelClearsNotification() {
		val sessionId = UUID.randomUUID()
		val session = TestSession(
			id = sessionId,
			initialState = Session.State.Active,
			notificationId = 42
		)
		val notificationManager = context.getSystemService<NotificationManager>()
		checkNotNull(notificationManager)
		val notification = NotificationCompat.Builder(context, "ackpine")
			.setContentTitle("title")
			.setContentText("text")
			.setSmallIcon(android.R.drawable.ic_dialog_alert)
			.build()
		notificationManager.notify(sessionId.toString(), 42, notification)

		session.cancel()
		idleMainThread()

		val shadowManager = shadowOf(notificationManager)
		assertEquals(0, shadowManager.allNotifications.size)
	}

	@Test
	fun terminalStateIgnoresSubsequentTransitions() {
		for (state in listOf(
			Session.State.Cancelled,
			Session.State.Succeeded,
			Session.State.Failed(TestFailure.Aborted(""))
		)) {
			val session = TestSession(initialState = state)
			val states = session.captureStates()
			idleMainThread()

			session.cancel()
			session.complete(Session.State.Succeeded)
			session.complete(Session.State.Failed(TestFailure.Aborted("aborted")))

			assertEquals(listOf(state), states)
		}
	}

	@Test
	fun completeSucceededMarksSucceeded() {
		val session = TestSession(initialState = Session.State.Committed)
		val states = session.captureStates()

		session.complete(Session.State.Succeeded)
		idleMainThread()

		assertEquals(Session.State.Succeeded, states.last())
	}

	@Test
	fun completeFailedPersistsFailure() {
		val failureDao = TestSessionFailureDao<TestFailure>()
		val sessionId = UUID.randomUUID()
		val session = TestSession(
			id = sessionId,
			failureDao = failureDao,
			initialState = Session.State.Committed
		)
		val states = session.captureStates()
		val failure = TestFailure.Aborted("User aborted")

		session.complete(Session.State.Failed(failure))
		idleMainThread()

		assertIs<Session.State.Failed<InstallFailure>>(states.last())
		assertEquals(failure, failureDao.getFailure(session.id.toString()))
	}

	@Test
	fun completeExceptionallySetsFailedAndPersistsFailure() {
		val failureDao = TestSessionFailureDao<TestFailure>()
		val session = TestSession(
			failureDao = failureDao,
			initialState = Session.State.Active
		)
		val states = session.captureStates()

		val exception = IllegalStateException("boom")
		session.completeExceptionally(exception)
		idleMainThread()

		assertIs<Session.State.Failed<TestFailure>>(states.last())
		val failure = failureDao.getFailure(session.id.toString())
		assertIs<TestFailure.Exceptional>(failure)
		assertEquals(failure.exception, exception)
	}

	private inner class TestSession(
		id: UUID = UUID.randomUUID(),
		sessionDao: RecordingSessionDao = RecordingSessionDao(),
		failureDao: TestSessionFailureDao<TestFailure> = TestSessionFailureDao(),
		initialState: Session.State<TestFailure>,
		notificationId: Int = 1
	) : AbstractSession<TestFailure>(
		context = context,
		id = id,
		initialState = initialState,
		sessionDao = sessionDao,
		sessionFailureDao = failureDao,
		executor = ImmediateExecutor,
		handler = handler,
		exceptionalFailureFactory = TestFailure::Exceptional,
		notificationId = notificationId,
		dbWriteSemaphore = dbWriteSemaphore
	) {

		var prepareCalls = 0
		var confirmationCalls = 0
		var committedCalls = 0

		override fun prepare() {
			prepareCalls++
			notifyAwaiting()
		}

		override fun launchConfirmation() {
			confirmationCalls++
			notifyCommitted()
		}

		override fun onCommitted() {
			committedCalls++
		}
	}

	private class SessionStateFlags<F : Failure>(
		val state: Session.State<F>,
		val isActive: Boolean,
		val isCancelled: Boolean,
		val isCompleted: Boolean
	)
}