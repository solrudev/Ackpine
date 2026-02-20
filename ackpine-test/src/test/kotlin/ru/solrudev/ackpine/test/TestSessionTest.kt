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

package ru.solrudev.ackpine.test

import ru.solrudev.ackpine.DisposableSubscriptionContainer
import ru.solrudev.ackpine.DummyDisposableSubscription
import ru.solrudev.ackpine.session.Failure
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.uninstaller.UninstallFailure
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

class TestSessionTest {

	@Test
	fun sessionIdPropertyReturnsCorrectId() {
		val expectedId = UUID.randomUUID()
		val session = TestSession(id = expectedId, initialState = Session.State.Pending)
		assertEquals(expectedId, session.id)
	}

	@Test
	fun stateFlagsReflectCurrentState() {
		val failedState = Session.State.Failed(UninstallFailure.Aborted(""))
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
	fun stateListenerReceivesCurrentState() {
		val session = TestSession(initialState = Session.State.Pending)
		session.updateState(Session.State.Awaiting)
		val states = session.captureStates()
		assertEquals(listOf(Session.State.Awaiting), states)
	}

	@Test
	fun addDuplicateStateListenerReturnsDummy() {
		val session = TestSession(initialState = Session.State.Pending)
		val container = DisposableSubscriptionContainer()
		val listener = Session.StateListener<Nothing> { _, _ -> }

		val first = session.addStateListener(container, listener)
		val dummy = session.addStateListener(container, listener)

		assertNotEquals(DummyDisposableSubscription, first)
		assertEquals(DummyDisposableSubscription, dummy)
	}

	@Test
	fun disposeStateSubscriptionRemovesListener() {
		val session = TestSession(initialState = Session.State.Pending)
		val states = mutableListOf<Session.State<*>>()
		val container = DisposableSubscriptionContainer()
		val listener = Session.StateListener<Nothing> { _, state -> states += state }

		val subscription = session.addStateListener(container, listener)
		subscription.dispose()

		session.launch()

		// Only initial notification, no Active/Awaiting notifications
		assertEquals(listOf<Session.State<*>>(Session.State.Pending), states)
		assertTrue(subscription.isDisposed)
	}

	@Test
	fun multipleListenersAllNotified() {
		val session = TestSession(TestSessionScript.empty(), initialState = Session.State.Pending)
		val states1 = session.captureStates()
		val states2 = session.captureStates()

		session.launch()

		assertEquals(states1, states2)
		val expectedStates = listOf(
			Session.State.Pending,
			Session.State.Active
		)
		assertEquals(expectedStates, states1)
	}

	@Test
	fun sameStateValueNotNotified() {
		for (state in listOf(
			Session.State.Pending,
			Session.State.Active,
			Session.State.Awaiting,
			Session.State.Committed,
			Session.State.Cancelled,
			Session.State.Succeeded,
			Session.State.Failed(UninstallFailure.Aborted(""))
		)) {
			val session = TestSession(TestSessionScript.empty(), initialState = state)
			val stateEvents = session.captureStates()

			session.updateState(state)

			assertEquals(listOf(state), stateEvents)
		}
	}

	@Test
	fun launchTransitionsFromPendingToActive() = testLaunch(
		initialState = Session.State.Pending,
		expectedStates = listOf(
			Session.State.Pending,
			Session.State.Active
		)
	)

	@Test
	fun launchFromActiveIsPermitted() = testLaunch(
		initialState = Session.State.Active,
		expectedStates = listOf(Session.State.Active)
	)

	private fun testLaunch(
		initialState: Session.State<Nothing>,
		expectedStates: List<Session.State<Nothing>>
	) {
		val session = TestSession(TestSessionScript.empty(), initialState = initialState)
		assertTrue(session.launch())
		assertFalse(session.launch())
		assertEquals(expectedStates, session.stateHistory)
	}

	@Test
	fun launchFromWrongStateReturnsFalse() {
		for (state in listOf(
			Session.State.Awaiting,
			Session.State.Committed,
			Session.State.Cancelled,
			Session.State.Succeeded,
			Session.State.Failed(UninstallFailure.Aborted(""))
		)) {
			val session = TestSession(TestSessionScript.empty(), initialState = state)
			assertFalse(session.launch())
		}
	}

	@Test
	fun commitTransitionsToCommitted() = testCommit(
		initialState = Session.State.Awaiting,
		expectedStates = listOf(
			Session.State.Awaiting,
			Session.State.Committed
		)
	)

	@Test
	fun commitFromCommittedIsPermitted() = testCommit(
		initialState = Session.State.Committed,
		expectedStates = listOf(Session.State.Committed)
	)

	private fun testCommit(
		initialState: Session.State<Nothing>,
		expectedStates: List<Session.State<Nothing>>
	) {
		val session = TestSession(TestSessionScript.empty(), initialState = initialState)
		assertTrue(session.commit())
		assertFalse(session.commit())
		assertEquals(expectedStates, session.stateHistory)
	}

	@Test
	fun commitFromWrongStateReturnsFalse() {
		for (state in listOf(
			Session.State.Pending,
			Session.State.Active,
			Session.State.Cancelled,
			Session.State.Succeeded,
			Session.State.Failed(UninstallFailure.Aborted(""))
		)) {
			val session = TestSession(TestSessionScript.empty(), initialState = state)
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
			val session = TestSession(TestSessionScript.empty(), initialState = state)
			session.cancel()
			assertEquals(Session.State.Cancelled, session.state)
		}
	}

	@Test
	fun terminalStateIgnoresSubsequentTransitions() {
		for (state in listOf(
			Session.State.Cancelled,
			Session.State.Succeeded,
			Session.State.Failed(UninstallFailure.Aborted(""))
		)) {
			val session = TestSession(TestSessionScript.empty(), initialState = state)

			session.cancel()
			session.updateState(Session.State.Succeeded)
			session.updateState(Session.State.Failed(UninstallFailure.Aborted("aborted")))

			assertEquals(listOf(state), session.stateHistory)
			assertEquals(state, session.state)
		}
	}

	@Test
	fun stateHistoryAppendsOnStateTransitions() {
		val failure = UninstallFailure.Aborted("")
		val session = TestUninstallSession(TestSessionScript.empty())

		session.updateState(Session.State.Active)
		session.updateState(Session.State.Awaiting)
		session.updateState(Session.State.Committed)
		session.updateState(Session.State.Failed(failure))

		val expectedStates = listOf(
			Session.State.Pending,
			Session.State.Active,
			Session.State.Awaiting,
			Session.State.Committed,
			Session.State.Failed(failure)
		)
		assertEquals(expectedStates, session.stateHistory)
	}

	@Test
	fun resetResetsStateAndHistory() {
		val session = TestUninstallSession(TestSessionScript.empty())
		val stateUpdates = session.captureStates()
		session.updateState(Session.State.Active)

		session.reset(state = Session.State.Awaiting, notifyListeners = false)
		assertEquals(Session.State.Awaiting, session.state)
		assertEquals(listOf(Session.State.Awaiting), session.stateHistory)
		assertEquals(listOf(Session.State.Pending, Session.State.Active), stateUpdates)

		session.reset(state = Session.State.Pending, notifyListeners = true)
		assertEquals(Session.State.Pending, session.state)
		assertEquals(listOf(Session.State.Pending), session.stateHistory)
		assertEquals(listOf(Session.State.Pending, Session.State.Active, Session.State.Pending), stateUpdates)
	}

	@Test
	fun stateHistoryReturnsSnapshotCopy() {
		val session = TestUninstallSession(TestSessionScript.empty())

		val snapshot1 = session.stateHistory
		session.updateState(Session.State.Active)
		val snapshot2 = session.stateHistory

		assertNotSame(snapshot1, snapshot2)
		assertNotEquals(snapshot1, snapshot2)
	}

	private class SessionStateFlags<F : Failure>(
		val state: Session.State<F>,
		val isActive: Boolean,
		val isCancelled: Boolean,
		val isCompleted: Boolean
	)
}