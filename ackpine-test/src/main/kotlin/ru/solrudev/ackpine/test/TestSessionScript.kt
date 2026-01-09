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
import java.util.ArrayDeque

/**
 * Scripted transitions used to drive session state updates in tests.
 *
 * Launch and commit transitions are queued and consumed on each respective call. The optional [cancelState]
 * is applied when [Session.cancel] is invoked.
 */
public class TestSessionScript<F : Failure> private constructor() {

	private val launchQueue = ArrayDeque<List<Session.State<F>>>()
	private val commitQueue = ArrayDeque<List<Session.State<F>>>()

	/**
	 * State that will be applied when [Session.cancel] is invoked.
	 */
	@get:JvmSynthetic
	internal var cancelState: Session.State<F>? = null
		private set

	/**
	 * Enqueues a sequence of states to apply after [Session.launch].
	 */
	public fun onLaunch(vararg states: Session.State<F>): TestSessionScript<F> = apply {
		if (states.isNotEmpty()) {
			launchQueue.addLast(states.toList())
		}
	}

	/**
	 * Enqueues a sequence of states to apply after [Session.commit].
	 */
	public fun onCommit(vararg states: Session.State<F>): TestSessionScript<F> = apply {
		if (states.isNotEmpty()) {
			commitQueue.addLast(states.toList())
		}
	}

	/**
	 * Sets a state to apply when [Session.cancel] is invoked.
	 */
	public fun onCancel(state: Session.State<F>?): TestSessionScript<F> = apply {
		cancelState = state
	}

	/**
	 * Clears any queued launch transitions.
	 */
	public fun clearLaunch(): TestSessionScript<F> = apply {
		launchQueue.clear()
	}

	/**
	 * Clears any queued commit transitions.
	 */
	public fun clearCommit(): TestSessionScript<F> = apply {
		commitQueue.clear()
	}

	/**
	 * Clears the cancel transition.
	 */
	public fun clearCancel(): TestSessionScript<F> = apply {
		cancelState = null
	}

	/**
	 * Clears all scripted transitions.
	 */
	public fun clear(): TestSessionScript<F> = apply {
		clearLaunch()
		clearCommit()
		clearCancel()
	}

	@JvmSynthetic
	internal fun nextLaunchStates(): List<Session.State<F>>? {
		return if (launchQueue.isEmpty()) null else launchQueue.removeFirst()
	}

	@JvmSynthetic
	internal fun nextCommitStates(): List<Session.State<F>>? {
		return if (commitQueue.isEmpty()) null else commitQueue.removeFirst()
	}

	/**
	 * Factories for [TestSessionScript].
	 */
	public companion object {

		/**
		 * Creates a standard [TestSessionScript] which advances session state on [Session.launch] and [Session.commit]
		 * calls normally until [terminalState] is reached.
		 */
		@JvmStatic
		public fun <S, F : Failure> auto(terminalState: S): TestSessionScript<F>
				where S : Session.State.Terminal,
					  S : Session.State<F> {
			return TestSessionScript<F>()
				.onLaunch(Session.State.Active, Session.State.Awaiting)
				.onCommit(Session.State.Committed, terminalState)
		}

		/**
		 * Creates a new empty [TestSessionScript].
		 *
		 * Use when scripting state transitions manually or when driving session's state through
		 * [TestSessionController].
		 */
		@JvmStatic
		public fun <F : Failure> empty(): TestSessionScript<F> = TestSessionScript()
	}
}