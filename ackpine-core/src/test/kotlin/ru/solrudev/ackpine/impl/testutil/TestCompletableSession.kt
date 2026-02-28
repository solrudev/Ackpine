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

package ru.solrudev.ackpine.impl.testutil

import ru.solrudev.ackpine.DisposableSubscription
import ru.solrudev.ackpine.DisposableSubscriptionContainer
import ru.solrudev.ackpine.DummyDisposableSubscription
import ru.solrudev.ackpine.impl.session.Cleanable
import ru.solrudev.ackpine.impl.session.CompletableProgressSession
import ru.solrudev.ackpine.impl.session.CompletableSession
import ru.solrudev.ackpine.session.Failure
import ru.solrudev.ackpine.session.Progress
import ru.solrudev.ackpine.session.ProgressSession
import ru.solrudev.ackpine.session.Session
import java.util.UUID
import java.util.concurrent.CopyOnWriteArraySet

internal open class TestCompletableSession<F : Failure>(
	override val id: UUID,
	initialState: Session.State<F> = Session.State.Pending,
	private val exceptionalFailureFactory: ((Exception) -> F)? = null
) : CompletableSession<F>, Cleanable {

	private val stateListeners = CopyOnWriteArraySet<Session.StateListener<F>>()

	@Volatile
	var state: Session.State<F> = initialState
		private set

	@Volatile
	var completedState: Session.State.Completed<F>? = null
		private set

	@Volatile
	var completionException: Exception? = null
		private set

	@Volatile
	var committedNotifiedCount: Int = 0
		private set

	@Volatile
	var cleanupCalls: Int = 0
		private set

	val committedNotified: Boolean
		get() = committedNotifiedCount > 0

	override val isActive: Boolean
		get() = state !is Session.State.Pending && !state.isTerminal

	override val isCompleted: Boolean
		get() = state is Session.State.Completed<*>

	override val isCancelled: Boolean
		get() = state is Session.State.Cancelled

	override fun launch() = true
	override fun commit() = true
	override fun cancel() = updateState(Session.State.Cancelled)

	override fun addStateListener(
		subscriptionContainer: DisposableSubscriptionContainer,
		listener: Session.StateListener<F>
	): DisposableSubscription {
		val added = stateListeners.add(listener)
		if (!added) {
			return DummyDisposableSubscription
		}
		listener.onStateChanged(id, state)
		val subscription = Subscription { stateListeners.remove(listener) }
		subscriptionContainer.add(subscription)
		return subscription
	}

	override fun removeStateListener(listener: Session.StateListener<F>) {
		stateListeners -= listener
	}

	override fun complete(state: Session.State.Completed<F>) {
		completedState = state
		updateState(state)
	}

	override fun completeExceptionally(exception: Exception) {
		completionException = exception
		val factory = exceptionalFailureFactory
			?: error("exceptionalFailureFactory must be provided to use completeExceptionally")
		updateState(Session.State.Failed(factory(exception)))
	}

	override fun notifyCommitted() {
		committedNotifiedCount++
	}

	override fun cleanup() {
		cleanupCalls++
	}

	fun updateState(state: Session.State<F>) {
		this.state = state
		for (listener in stateListeners) {
			listener.onStateChanged(id, state)
		}
	}

	internal class Subscription(private val onDispose: () -> Unit) : DisposableSubscription {

		override var isDisposed: Boolean = false
			private set

		override fun dispose() {
			if (!isDisposed) {
				onDispose()
				isDisposed = true
			}
		}
	}
}

internal open class TestCompletableProgressSession<F : Failure>(
	id: UUID,
	initialState: Session.State<F> = Session.State.Pending,
	exceptionalFailureFactory: ((Exception) -> F)? = null
) : TestCompletableSession<F>(id, initialState, exceptionalFailureFactory), CompletableProgressSession<F> {

	@Volatile
	var progress: Progress = Progress()
		private set

	private val progressListeners = CopyOnWriteArraySet<ProgressSession.ProgressListener>()

	override fun addProgressListener(
		subscriptionContainer: DisposableSubscriptionContainer,
		listener: ProgressSession.ProgressListener
	): DisposableSubscription {
		val added = progressListeners.add(listener)
		if (!added) {
			return DummyDisposableSubscription
		}
		listener.onProgressChanged(id, progress)
		val subscription = Subscription { progressListeners.remove(listener) }
		subscriptionContainer.add(subscription)
		return subscription
	}

	override fun removeProgressListener(listener: ProgressSession.ProgressListener) {
		progressListeners -= listener
	}

	fun notifyProgress(newProgress: Progress) {
		progress = newProgress
		for (listener in progressListeners) {
			listener.onProgressChanged(id, newProgress)
		}
	}
}