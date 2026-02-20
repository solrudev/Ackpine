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

import ru.solrudev.ackpine.session.Failure
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.uninstaller.UninstallFailure
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TestSessionScriptTest {

	@Test
	fun autoEnqueuesExpectedLaunchAndCommitTransitions() {
		val script = TestSessionScript.auto(Session.State.Succeeded)

		assertEquals(listOf(Session.State.Awaiting), script.nextLaunchStates())
		assertEquals(listOf(Session.State.Succeeded), script.nextCommitStates())
		assertTrue(script.nextLaunchStates().isEmpty())
		assertTrue(script.nextCommitStates().isEmpty())
	}

	@Test
	fun emptyHasNoQueuedTransitions() {
		val script = TestSessionScript.empty<Failure>()

		assertTrue(script.nextLaunchStates().isEmpty())
		assertTrue(script.nextCommitStates().isEmpty())
		assertNull(script.cancelState)
	}

	@Test
	fun onLaunchIgnoresEmptyVarargs() {
		val script = TestSessionScript.empty<Failure>().onLaunch()
		assertTrue(script.nextLaunchStates().isEmpty())
	}

	@Test
	fun onCommitIgnoresEmptyVarargs() {
		val script = TestSessionScript.empty<Failure>().onCommit()
		assertTrue(script.nextCommitStates().isEmpty())
	}

	@Test
	fun clearOperationsAndOnCancelBehavior() {
		val failure = UninstallFailure.Aborted("")
		val script = TestSessionScript.empty<UninstallFailure>()
			.onLaunch(Session.State.Awaiting)
			.onCommit(Session.State.Committed)
			.onCancel(Session.State.Failed(failure))

		assertEquals(listOf(Session.State.Awaiting), script.nextLaunchStates())
		assertEquals(listOf(Session.State.Committed), script.nextCommitStates())
		assertEquals(Session.State.Failed(failure), script.cancelState)

		script
			.onLaunch(Session.State.Active)
			.onCommit(Session.State.Awaiting)
			.onCancel(Session.State.Cancelled)
			.clearLaunch()
			.clearCommit()
			.clearCancel()

		assertTrue(script.nextLaunchStates().isEmpty())
		assertTrue(script.nextCommitStates().isEmpty())
		assertNull(script.cancelState)

		script
			.onLaunch(Session.State.Active)
			.onCommit(Session.State.Awaiting)
			.onCancel(Session.State.Cancelled)
			.clear()

		assertTrue(script.nextLaunchStates().isEmpty())
		assertTrue(script.nextCommitStates().isEmpty())
		assertNull(script.cancelState)
	}

	@Test
	fun copyDeepCopiesQueuesAndCancelState() {
		val failure = UninstallFailure.Aborted("")
		val original = TestSessionScript.empty<UninstallFailure>()
			.onLaunch(Session.State.Active, Session.State.Awaiting)
			.onLaunch(Session.State.Committed)
			.onCommit(Session.State.Awaiting)
			.onCommit(Session.State.Failed(failure))
			.onCancel(Session.State.Cancelled)

		val copy = original.copy()

		assertEquals(original.nextLaunchStates(), copy.nextLaunchStates())
		assertEquals(original.nextLaunchStates(), copy.nextLaunchStates())
		assertEquals(original.nextCommitStates(), copy.nextCommitStates())
		assertEquals(original.nextCommitStates(), copy.nextCommitStates())
		assertEquals(Session.State.Cancelled, original.cancelState)
		assertEquals(Session.State.Cancelled, copy.cancelState)

		original.onLaunch(Session.State.Awaiting).onCancel(null)

		assertTrue(copy.nextLaunchStates().isEmpty())
		assertEquals(Session.State.Cancelled, copy.cancelState)
	}

	@Test
	fun nextLaunchAndCommitStatesConsumeQueuesInFifo() {
		val failure = UninstallFailure.Aborted("")
		val script = TestSessionScript.empty<UninstallFailure>()
			.onLaunch(Session.State.Active)
			.onLaunch(Session.State.Awaiting)
			.onCommit(Session.State.Committed)
			.onCommit(Session.State.Failed(failure))

		assertEquals(listOf(Session.State.Active), script.nextLaunchStates())
		assertEquals(listOf(Session.State.Awaiting), script.nextLaunchStates())
		assertTrue(script.nextLaunchStates().isEmpty())

		assertEquals(listOf(Session.State.Committed), script.nextCommitStates())
		assertEquals(listOf(Session.State.Failed(failure)), script.nextCommitStates())
		assertTrue(script.nextCommitStates().isEmpty())
	}
}