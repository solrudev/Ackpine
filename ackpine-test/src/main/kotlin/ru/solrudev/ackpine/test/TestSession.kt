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

import ru.solrudev.ackpine.DisposableSubscription
import ru.solrudev.ackpine.DisposableSubscriptionContainer
import ru.solrudev.ackpine.DummyDisposableSubscription
import ru.solrudev.ackpine.session.Failure
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.session.Session.State.Active
import ru.solrudev.ackpine.session.Session.State.Awaiting
import ru.solrudev.ackpine.session.Session.State.Cancelled
import ru.solrudev.ackpine.session.Session.State.Committed
import ru.solrudev.ackpine.session.Session.State.Pending
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CopyOnWriteArraySet

/**
 * A controllable [Session] test double.
 *
 * State listeners are invoked on the calling thread, and the current state is delivered immediately when a
 * listener is added. Use [controller] to drive state transitions directly or to script transitions tied to
 * [Session.launch] and [Session.commit] calls.
 */
public open class TestSession<F : Failure> @JvmOverloads public constructor(
	script: TestSessionScript<F> = TestSessionScript.auto(Session.State.Succeeded),
	override val id: UUID = UUID.randomUUID(),
	initialState: Session.State<F> = Pending
) : Session<F> {

	private val stateListeners = CopyOnWriteArraySet<Session.StateListener<F>>()
	private val stateLock = Any()
	private val stateHistoryValues = CopyOnWriteArrayList<Session.State<F>>().apply { add(initialState) }

	/**
	 * Returns the current state of this session.
	 */
	@Volatile
	public var state: Session.State<F> = initialState
		private set

	/**
	 * Returns a snapshot of state transitions for this session.
	 */
	public val stateHistory: List<Session.State<F>>
		get() = stateHistoryValues.toList()

	/**
	 * Returns the controller used to drive this session in tests.
	 */
	public val controller: TestSessionController<F> = TestSessionController.create(this, script)

	override val isActive: Boolean
		get() = state.let { it !is Pending && !it.isTerminal }

	override val isCompleted: Boolean
		get() = state is Session.State.Completed<*>

	override val isCancelled: Boolean
		get() = state is Cancelled

	override fun launch(): Boolean {
		val currentState = state
		if (currentState.isTerminal) {
			return false
		}
		if (currentState !is Pending && currentState !is Active) {
			return false
		}
		if (currentState is Pending) {
			updateState(Active)
		}
		controller.handleLaunch()
		return true
	}

	override fun commit(): Boolean {
		val currentState = state
		if (currentState.isTerminal) {
			return false
		}
		if (currentState !is Awaiting && currentState !is Committed) {
			return false
		}
		if (currentState is Awaiting) {
			updateState(Committed)
		}
		controller.handleCommit()
		return true
	}

	override fun cancel() {
		if (state.isTerminal) {
			return
		}
		controller.handleCancel()
	}

	override fun addStateListener(
		subscriptionContainer: DisposableSubscriptionContainer,
		listener: Session.StateListener<F>
	): DisposableSubscription {
		val added = stateListeners.add(listener)
		if (!added) {
			return DummyDisposableSubscription
		}
		listener.onStateChanged(id, state)
		val subscription = StateDisposableSubscription(this, listener)
		subscriptionContainer.add(subscription)
		return subscription
	}

	override fun removeStateListener(listener: Session.StateListener<F>) {
		stateListeners -= listener
	}

	/**
	 * Resets state and state history to the provided [state].
	 * @param state a [Session.State] to set. [Pending] by default
	 * @param notifyListeners whether to notify listeners (including `await()`) with the newly set state. `false` by
	 * default.
	 */
	@JvmOverloads
	public open fun reset(state: Session.State<F> = Pending, notifyListeners: Boolean = false) {
		synchronized(stateLock) {
			this.state = state
		}
		stateHistoryValues.clear()
		stateHistoryValues.add(state)
		if (notifyListeners) {
			notifyStateListeners(state)
		}
	}

	@JvmSynthetic
	internal fun updateState(newState: Session.State<F>) {
		synchronized(stateLock) {
			val currentState = state
			if (currentState == newState || currentState.isTerminal) {
				return
			}
			state = newState
		}
		stateHistoryValues.add(newState)
		notifyStateListeners(newState)
	}

	private fun notifyStateListeners(state: Session.State<F>) {
		for (listener in stateListeners) {
			listener.onStateChanged(id, state)
		}
	}

	private class StateDisposableSubscription<F : Failure>(
		private val session: TestSession<F>,
		private val listener: Session.StateListener<F>
	) : DisposableSubscription {

		override var isDisposed: Boolean = false
			private set

		override fun dispose() {
			if (!isDisposed) {
				session.removeStateListener(listener)
				isDisposed = true
			}
		}
	}
}