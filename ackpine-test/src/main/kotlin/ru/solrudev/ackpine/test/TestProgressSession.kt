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
import ru.solrudev.ackpine.session.Progress
import ru.solrudev.ackpine.session.ProgressSession
import ru.solrudev.ackpine.session.ProgressSession.ProgressListener
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.session.Session.State.Pending
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CopyOnWriteArraySet

/**
 * A controllable [ProgressSession] test double.
 *
 * Progress listeners are invoked on the calling thread, and the current progress is delivered immediately when a
 * listener is added. Use [controller] to drive progress updates alongside state transitions.
 */
public class TestProgressSession<F : Failure> @JvmOverloads public constructor(
	script: TestSessionScript<F> = TestSessionScript.auto(Session.State.Succeeded),
	id: UUID = UUID.randomUUID(),
	initialState: Session.State<F> = Pending,
	private val initialProgress: Progress = Progress()
) : TestSession<F>(script, id, initialState), ProgressSession<F> {

	private val progressListeners = CopyOnWriteArraySet<ProgressListener>()
	private val progressHistoryValues = CopyOnWriteArrayList<Progress>().apply { add(initialProgress) }

	/**
	 * Returns the current progress of this session.
	 */
	@Volatile
	public var progress: Progress = initialProgress
		private set

	/**
	 * Returns a snapshot of progress updates for this session.
	 */
	public val progressHistory: List<Progress>
		get() = progressHistoryValues.toList()

	override val controller: TestProgressSessionController<F> = TestProgressSessionController.create(this, script)

	override fun addProgressListener(
		subscriptionContainer: DisposableSubscriptionContainer,
		listener: ProgressListener
	): DisposableSubscription {
		val added = progressListeners.add(listener)
		if (!added) {
			return DummyDisposableSubscription
		}
		listener.onProgressChanged(id, progress)
		val subscription = ProgressDisposableSubscription(this, listener)
		subscriptionContainer.add(subscription)
		return subscription
	}

	override fun removeProgressListener(listener: ProgressListener) {
		progressListeners -= listener
	}

	/**
	 * Resets progress and progress history to the provided [progress].
	 * @param progress a [Progress] to set. Defaults to the provided initial progress value.
	 * @param notifyListeners whether to notify listeners (including `Session.progress` flow) with the newly set
	 * progress. `false` by default.
	 */
	@JvmOverloads
	public fun resetProgress(progress: Progress = initialProgress, notifyListeners: Boolean = false) {
		this.progress = progress
		progressHistoryValues.clear()
		progressHistoryValues.add(progress)
		if (notifyListeners) {
			notifyProgressListeners(progress)
		}
	}

	override fun reset(state: Session.State<F>, notifyListeners: Boolean) {
		super.reset(state, notifyListeners)
		resetProgress(initialProgress, notifyListeners)
	}

	@JvmSynthetic
	internal fun updateProgress(progress: Progress) {
		if (this.progress == progress) {
			return
		}
		this.progress = progress
		progressHistoryValues.add(progress)
		notifyProgressListeners(progress)
	}

	private fun notifyProgressListeners(progress: Progress) {
		for (listener in progressListeners) {
			listener.onProgressChanged(id, progress)
		}
	}

	private class ProgressDisposableSubscription<F : Failure>(
		private val session: TestProgressSession<F>,
		private val listener: ProgressListener
	) : DisposableSubscription {

		override var isDisposed: Boolean = false
			private set

		override fun dispose() {
			if (!isDisposed) {
				session.removeProgressListener(listener)
				isDisposed = true
			}
		}
	}
}