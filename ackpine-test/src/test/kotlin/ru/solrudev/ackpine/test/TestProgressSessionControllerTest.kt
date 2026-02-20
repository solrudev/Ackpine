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
import kotlin.test.assertSame

class TestProgressSessionControllerTest {

	@Test
	fun controllerUpdatesSessionStateAndReturnsItself() {
		val session = TestProgressSession<UninstallFailure>(TestSessionScript.empty())
		val failure = UninstallFailure.Generic("failure")

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
	fun setProgressUpdatesProgressHistoryAndNotifiesListeners() {
		val session = TestProgressSession<UninstallFailure>(TestSessionScript.empty())
		val controller = session.controller
		val progressUpdates = session.captureProgress()

		assertSame(controller, controller.setProgress(Progress(10, 20)))
		assertEquals(Progress(10, 20), session.progress)
		assertEquals(listOf(Progress(), Progress(10, 20)), session.progressHistory)
		assertEquals(listOf(Progress(), Progress(10, 20)), progressUpdates)
	}

	@Test
	fun resetCallsResetsAllCounters() {
		val session = TestProgressSession(TestSessionScript.empty())

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
}