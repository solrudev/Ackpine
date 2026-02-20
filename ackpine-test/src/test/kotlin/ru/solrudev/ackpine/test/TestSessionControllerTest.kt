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

import ru.solrudev.ackpine.session.Progress
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.uninstaller.UninstallFailure
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class TestSessionControllerTest {

	@Test
	fun controllerUpdatesSessionStateAndReturnsItself() {
		val session = TestSession(TestSessionScript.empty())
		val failure = UninstallFailure.Aborted("")

		assertSame(session.controller, session.controller.setState(Session.State.Active))
		assertEquals(Session.State.Active, session.state)

		assertSame(session.controller, session.controller.succeed())
		assertEquals(Session.State.Succeeded, session.state)

		session.reset()
		assertSame(session.controller, session.controller.fail(failure))
		assertEquals(Session.State.Failed(failure), session.state)

		session.reset()
		assertSame(session.controller, session.controller.cancel())
		assertEquals(Session.State.Cancelled, session.state)
	}

	@Test
	fun launchCommitCancelIncrementCounters() {
		val session = TestSession(TestSessionScript.empty())

		assertEquals(0, session.controller.launchCalls)
		assertEquals(0, session.controller.commitCalls)
		assertEquals(0, session.controller.cancelCalls)

		session.launch()
		assertEquals(1, session.controller.launchCalls)

		session.controller.setState(Session.State.Awaiting)
		session.commit()
		assertEquals(1, session.controller.commitCalls)

		session.cancel()
		assertEquals(1, session.controller.cancelCalls)
	}

	@Test
	fun resetCallsResetsAllCounters() {
		val session = TestSession(TestSessionScript.empty())

		session.launch()
		session.controller.setState(Session.State.Awaiting)
		session.commit()
		session.reset()
		session.cancel()
		assertEquals(1, session.controller.launchCalls)
		assertEquals(1, session.controller.commitCalls)
		assertEquals(1, session.controller.cancelCalls)

		assertSame(session.controller, session.controller.resetCalls())
		assertEquals(0, session.controller.launchCalls)
		assertEquals(0, session.controller.commitCalls)
		assertEquals(0, session.controller.cancelCalls)
	}

	@Test
	fun scriptedStateApplicationOrderOnLaunchAndCommit() {
		val failure = UninstallFailure.Aborted("")
		val script = TestSessionScript.empty<UninstallFailure>()
			.onLaunch(Session.State.Awaiting, Session.State.Committed)
			.onCommit(Session.State.Awaiting, Session.State.Failed(failure))
		val session = TestSession(script)

		session.launch()
		val postLaunchExpectedStates = listOf(
			Session.State.Pending,
			Session.State.Active,
			// Scripted
			Session.State.Awaiting,
			Session.State.Committed
		)
		assertEquals(postLaunchExpectedStates, session.stateHistory)

		session.controller.setState(Session.State.Awaiting)
		session.commit()
		val postCommitExpectedStates = postLaunchExpectedStates + listOf(
			Session.State.Awaiting,
			Session.State.Committed,
			// Scripted
			Session.State.Awaiting,
			Session.State.Failed(failure)
		)
		assertEquals(postCommitExpectedStates, session.stateHistory)
	}

	@Test
	fun cancelUsesScriptedCancelStateWhenPresent() {
		val scriptedState = Session.State.Failed(UninstallFailure.Aborted(""))
		val script = TestSessionScript.empty<UninstallFailure>().onCancel(scriptedState)
		val scriptedSession = TestSession(script)

		scriptedSession.cancel()

		assertEquals(scriptedState, scriptedSession.state)
	}

	@Test
	fun cancelUsesCancelledStateWhenCancelStateIsAbsent() {
		val defaultSession = TestSession<UninstallFailure>(TestSessionScript.empty())
		defaultSession.cancel()
		assertEquals(Session.State.Cancelled, defaultSession.state)
	}

	@Test
	@Suppress("DEPRECATION_ERROR")
	fun deprecatedBaseSetProgressOnNonProgressSessionFailsWithExpectedError() {
		val session = TestSession<UninstallFailure>(TestSessionScript.empty())
		val exception = assertFailsWith<IllegalStateException> {
			session.controller.setProgress(Progress())
		}
		assertEquals("Progress can only be set for TestProgressSession.", exception.message)
	}
}