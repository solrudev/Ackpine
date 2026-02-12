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
import ru.solrudev.ackpine.session.Progress
import ru.solrudev.ackpine.session.Session

/**
 * Drives a [TestProgressSession] state and progress in tests.
 *
 * Use [setState], [succeed], [fail], or [cancel] to force transitions directly. Scripted transitions configured via
 * [TestSessionScript] are applied when the session methods are invoked, including via `await()` or
 * [Session.TerminalStateListener.bind].
 */
public class TestProgressSessionController<F : Failure> private constructor(
	private val session: TestProgressSession<F>,
	script: TestSessionScript<F>
) : TestSessionController<F>(session, script) {

	override fun resetCalls(): TestProgressSessionController<F> = apply { super.resetCalls() }

	override fun setState(state: Session.State<F>): TestProgressSessionController<F> = apply {
		session.updateState(state)
	}

	override fun succeed(): TestProgressSessionController<F> = setState(Session.State.Succeeded)
	override fun fail(failure: F): TestProgressSessionController<F> = setState(Session.State.Failed(failure))
	override fun cancel(): TestProgressSessionController<F> = setState(Session.State.Cancelled)

	/**
	 * Updates session progress.
	 */
	public fun setProgress(progress: Progress): TestProgressSessionController<F> = apply {
		session.updateProgress(progress)
	}

	internal companion object {
		@JvmSynthetic
		internal fun <F : Failure> create(
			session: TestProgressSession<F>,
			script: TestSessionScript<F>
		) = TestProgressSessionController(session, script)
	}
}