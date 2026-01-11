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
import java.util.concurrent.atomic.AtomicInteger

/**
 * Drives a [TestSession] state and progress in tests.
 *
 * Use [setState], [succeed], [fail], or [cancel] to force transitions directly. Scripted transitions configured via
 * [TestSessionScript] are applied when the session methods are invoked, including via `await()` or
 * [Session.TerminalStateListener.bind].
 */
public class TestSessionController<F : Failure> private constructor(
	private val session: TestSession<F>,
	private val script: TestSessionScript<F>
) {

	private val launchCallsValue = AtomicInteger(0)
	private val commitCallsValue = AtomicInteger(0)
	private val cancelCallsValue = AtomicInteger(0)

	/**
	 * Number of times [Session.launch] has been invoked.
	 */
	public val launchCalls: Int
		get() = launchCallsValue.get()

	/**
	 * Number of times [Session.commit] has been invoked.
	 */
	public val commitCalls: Int
		get() = commitCallsValue.get()

	/**
	 * Number of times [Session.cancel] has been invoked.
	 */
	public val cancelCalls: Int
		get() = cancelCallsValue.get()

	/**
	 * Resets launch/commit/cancel invocation counters.
	 */
	public fun resetCalls(): TestSessionController<F> = apply {
		launchCallsValue.set(0)
		commitCallsValue.set(0)
		cancelCallsValue.set(0)
	}

	/**
	 * Sets the current state of the session.
	 */
	public fun setState(state: Session.State<F>): TestSessionController<F> = apply {
		session.updateState(state)
	}

	/**
	 * Marks the session as succeeded.
	 */
	public fun succeed(): TestSessionController<F> = setState(Session.State.Succeeded)

	/**
	 * Marks the session as failed with the provided [failure].
	 */
	public fun fail(failure: F): TestSessionController<F> = setState(Session.State.Failed(failure))

	/**
	 * Marks the session as cancelled.
	 */
	public fun cancel(): TestSessionController<F> = setState(Session.State.Cancelled)

	/**
	 * Updates session progress, if supported.
	 */
	public fun setProgress(progress: Progress): TestSessionController<F> = apply {
		val progressSession = session as? TestProgressSession<F>
			?: error("Progress can only be set for TestProgressSession.")
		progressSession.updateProgress(progress)
	}

	@JvmSynthetic
	internal fun handleLaunch() {
		launchCallsValue.incrementAndGet()
		applyStates(script.nextLaunchStates())
	}

	@JvmSynthetic
	internal fun handleCommit() {
		commitCallsValue.incrementAndGet()
		applyStates(script.nextCommitStates())
	}

	@JvmSynthetic
	internal fun handleCancel() {
		cancelCallsValue.incrementAndGet()
		session.updateState(script.cancelState ?: Session.State.Cancelled)
	}

	private fun applyStates(states: List<Session.State<F>>?) {
		if (states == null) {
			return
		}
		for (state in states) {
			session.updateState(state)
		}
	}

	internal companion object {
		@JvmSynthetic
		internal fun <F : Failure> create(
			session: TestSession<F>,
			script: TestSessionScript<F>
		) = TestSessionController(session, script)
	}
}