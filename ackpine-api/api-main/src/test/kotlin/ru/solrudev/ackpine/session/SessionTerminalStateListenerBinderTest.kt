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

package ru.solrudev.ackpine.session

import ru.solrudev.ackpine.DisposableSubscriptionContainer
import ru.solrudev.ackpine.installer.InstallFailure
import ru.solrudev.ackpine.test.TestSession
import ru.solrudev.ackpine.test.TestSessionScript
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SessionTerminalStateListenerBinderTest {

	@Test
	fun binderInvokesSuccessListenerWhenRegisteredBeforeTerminalState() {
		val session = TestSession<Nothing>(TestSessionScript.empty(), id = UUID.randomUUID())
		val binder = Session.TerminalStateListener.bind(session, DisposableSubscriptionContainer())
		var actualSessionId: UUID? = null

		binder.addOnSuccessListener { sessionId -> actualSessionId = sessionId }
		session.controller.succeed()

		assertEquals(session.id, actualSessionId)
	}

	@Test
	fun binderInvokesFailureListenerWhenRegisteredBeforeTerminalState() {
		val session = TestSession<InstallFailure>(TestSessionScript.empty(), id = UUID.randomUUID())
		val binder = Session.TerminalStateListener.bind(session, DisposableSubscriptionContainer())
		val failure = InstallFailure.Aborted("")
		var actualSessionId: UUID? = null
		var actualFailure: InstallFailure? = null

		binder.addOnFailureListener { sessionId, stateFailure ->
			actualSessionId = sessionId
			actualFailure = stateFailure
		}
		session.controller.fail(failure)

		assertEquals(session.id, actualSessionId)
		assertEquals(failure, actualFailure)
	}

	@Test
	fun binderInvokesCancelListenerWhenRegisteredBeforeTerminalState() {
		val session = TestSession<Nothing>(TestSessionScript.empty(), id = UUID.randomUUID())
		val binder = Session.TerminalStateListener.bind(session, DisposableSubscriptionContainer())
		var actualSessionId: UUID? = null

		binder.addOnCancelListener { sessionId -> actualSessionId = sessionId }
		session.cancel()

		assertEquals(session.id, actualSessionId)
	}

	@Test
	fun binderInvokesSuccessListenerImmediatelyWhenRegisteredAfterSucceeded() {
		val session = TestSession<Nothing>(TestSessionScript.empty(), id = UUID.randomUUID())
		val binder = Session.TerminalStateListener.bind(session, DisposableSubscriptionContainer())
		session.controller.succeed()
		var actualSessionId: UUID? = null

		binder.addOnSuccessListener { sessionId -> actualSessionId = sessionId }

		assertEquals(session.id, actualSessionId)
	}

	@Test
	fun binderInvokesFailureListenerImmediatelyWhenRegisteredAfterFailed() {
		val session = TestSession<InstallFailure>(TestSessionScript.empty(), id = UUID.randomUUID())
		val binder = Session.TerminalStateListener.bind(session, DisposableSubscriptionContainer())
		val failure = InstallFailure.Aborted("")
		session.controller.fail(failure)
		var actualSessionId: UUID? = null
		var actualFailure: InstallFailure? = null

		binder.addOnFailureListener { sessionId, stateFailure ->
			actualSessionId = sessionId
			actualFailure = stateFailure
		}

		assertEquals(session.id, actualSessionId)
		assertEquals(failure, actualFailure)
	}

	@Test
	fun binderInvokesCancelListenerImmediatelyWhenRegisteredAfterCancelled() {
		val session = TestSession<Nothing>(TestSessionScript.empty(), id = UUID.randomUUID())
		val binder = Session.TerminalStateListener.bind(session, DisposableSubscriptionContainer())
		session.cancel()
		var actualSessionId: UUID? = null

		binder.addOnCancelListener { sessionId -> actualSessionId = sessionId }

		assertEquals(session.id, actualSessionId)
	}

	@Test
	fun binderReplacesSuccessListenerAndInvokesOnlyLatest() {
		val session = TestSession<Nothing>(TestSessionScript.empty(), id = UUID.randomUUID())
		val binder = Session.TerminalStateListener.bind(session, DisposableSubscriptionContainer())
		var firstInvocations = 0
		var secondInvocations = 0

		binder.addOnSuccessListener { firstInvocations++ }
		binder.addOnSuccessListener { secondInvocations++ }
		session.controller.succeed()

		assertEquals(0, firstInvocations)
		assertEquals(1, secondInvocations)
	}

	@Test
	fun binderReplacesFailureListenerAndInvokesOnlyLatest() {
		val session = TestSession<InstallFailure>(TestSessionScript.empty(), id = UUID.randomUUID())
		val binder = Session.TerminalStateListener.bind(session, DisposableSubscriptionContainer())
		val failure = InstallFailure.Aborted("")
		var firstFailure: InstallFailure? = null
		var secondFailure: InstallFailure? = null

		binder.addOnFailureListener { _, stateFailure -> firstFailure = stateFailure }
		binder.addOnFailureListener { _, stateFailure -> secondFailure = stateFailure }
		session.controller.fail(failure)

		assertNull(firstFailure)
		assertEquals(failure, secondFailure)
	}

	@Test
	fun binderReplacesCancelListenerAndInvokesOnlyLatest() {
		val session = TestSession<Nothing>(TestSessionScript.empty(), id = UUID.randomUUID())
		val binder = Session.TerminalStateListener.bind(session, DisposableSubscriptionContainer())
		var firstInvocations = 0
		var secondInvocations = 0

		binder.addOnCancelListener { firstInvocations++ }
		binder.addOnCancelListener { secondInvocations++ }
		session.cancel()

		assertEquals(0, firstInvocations)
		assertEquals(1, secondInvocations)
	}
}