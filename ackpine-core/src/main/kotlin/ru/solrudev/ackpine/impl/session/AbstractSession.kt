/*
 * Copyright (C) 2023 Ilya Fomichev
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

package ru.solrudev.ackpine.impl.session

import android.app.NotificationManager
import android.content.Context
import android.os.CancellationSignal
import android.os.Handler
import android.os.OperationCanceledException
import androidx.annotation.RestrictTo
import androidx.annotation.WorkerThread
import androidx.core.content.getSystemService
import ru.solrudev.ackpine.DisposableSubscription
import ru.solrudev.ackpine.DisposableSubscriptionContainer
import ru.solrudev.ackpine.DummyDisposableSubscription
import ru.solrudev.ackpine.impl.database.dao.SessionDao
import ru.solrudev.ackpine.impl.database.dao.SessionFailureDao
import ru.solrudev.ackpine.impl.database.model.SessionEntity
import ru.solrudev.ackpine.impl.helpers.concurrent.BinarySemaphore
import ru.solrudev.ackpine.impl.helpers.concurrent.withPermit
import ru.solrudev.ackpine.session.Failure
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.session.Session.State.Active
import ru.solrudev.ackpine.session.Session.State.Awaiting
import ru.solrudev.ackpine.session.Session.State.Cancelled
import ru.solrudev.ackpine.session.Session.State.Committed
import ru.solrudev.ackpine.session.Session.State.Completed
import ru.solrudev.ackpine.session.Session.State.Failed
import ru.solrudev.ackpine.session.Session.State.Pending
import ru.solrudev.ackpine.session.Session.State.Succeeded
import java.lang.ref.WeakReference
import java.util.Collections
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * A base implementation for Ackpine [sessions][Session].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal abstract class AbstractSession<F : Failure> protected constructor(
	private val context: Context,
	override val id: UUID,
	initialState: Session.State<F>,
	private val sessionDao: SessionDao,
	private val sessionFailureDao: SessionFailureDao<F>,
	private val executor: Executor,
	private val handler: Handler,
	private val exceptionalFailureFactory: (Exception) -> F,
	protected val notificationId: Int,
	private val dbWriteSemaphore: BinarySemaphore
) : CompletableSession<F>, Cleanable {

	protected val cancellationSignal = CancellationSignal()

	private val stateListeners = Collections.newSetFromMap(
		ConcurrentHashMap<Session.StateListener<F>, Boolean>()
	)

	private val isCancelling = AtomicBoolean(false)

	private val stateSnapshot = AtomicReference(
		StateSnapshot(
			state = initialState,
			isPreparing = false,
			isCommitCalled = false
		)
	)

	protected var state: Session.State<F>
		get() = stateSnapshot.get().state
		private set(value) {
			val shouldNotify = updateState { current ->
				val updatedState = if (current.state.isTerminal) current.state else value
				StateUpdate(
					newState = StateSnapshot(
						state = updatedState,
						isPreparing = false,
						isCommitCalled = false
					),
					result = current.state != updatedState
				)
			}
			if (shouldNotify) {
				notifyStateListeners(value)
			}
		}

	final override val isActive: Boolean
		get() = state.let { it !is Pending && !it.isTerminal }

	final override val isCompleted: Boolean
		get() = state is Completed

	final override val isCancelled: Boolean
		get() = state is Cancelled || isCancelling.get()

	/**
	 * Prepare the session. This method is called on a worker thread. After preparations are done, [notifyAwaiting] must
	 * be called.
	 */
	@WorkerThread
	protected abstract fun prepare()

	/**
	 * Launch session's confirmation. This method is called on a worker thread.
	 */
	@WorkerThread
	protected abstract fun launchConfirmation()

	/**
	 * Release any held resources after session's completion or cancellation.
	 */
	@WorkerThread
	protected open fun doCleanup() { /* optional */ }

	/**
	 * Notifies that preparations are done and sets session's state to [Awaiting].
	 */
	protected fun notifyAwaiting() {
		state = Awaiting
	}

	/**
	 * This callback method is invoked when the session's been committed. Processing in this method should be
	 * lightweight.
	 */
	protected open fun onCommitted() { /* optional */ }

	/**
	 * This callback method is invoked when the session's been [completed][Session.isCompleted]. Processing in
	 * this method should be lightweight.
	 * @return `true` if completion was handled.
	 */
	protected open fun onCompleted(state: Completed<F>): Boolean = true

	final override fun launch(): Boolean {
		if (!tryEnterLaunch()) {
			return false
		}
		executor.execute {
			if (!canPrepare()) {
				return@execute
			}
			try {
				sessionDao.updateLastLaunchTimestamp(id.toString(), System.currentTimeMillis())
				prepare()
			} catch (_: OperationCanceledException) { // no-op
			} catch (exception: Exception) {
				completeExceptionally(exception)
			}
		}
		return true
	}

	final override fun commit(): Boolean {
		if (!tryEnterCommit()) {
			return false
		}
		executor.execute {
			if (!canCommit()) {
				return@execute
			}
			try {
				launchConfirmation()
			} catch (_: OperationCanceledException) { // no-op
			} catch (exception: Exception) {
				completeExceptionally(exception)
			}
		}
		return true
	}

	final override fun cancel() {
		if (state.isTerminal || !isCancelling.compareAndSet(false, true)) {
			return
		}
		try {
			cancellationSignal.cancel()
			state = Cancelled
		} catch (exception: Exception) {
			completeExceptionally(exception)
		} finally {
			isCancelling.set(false)
		}
	}

	final override fun addStateListener(
		subscriptionContainer: DisposableSubscriptionContainer,
		listener: Session.StateListener<F>
	): DisposableSubscription {
		val added = stateListeners.add(listener)
		if (!added) {
			return DummyDisposableSubscription
		}
		// postAtFrontOfQueue - notify with current state snapshot immediately to avoid duplicate notifications,
		// as using plain Handler#post() can lead to the listener being notified after state has already changed
		// and delivered to the same listener
		handler.postAtFrontOfQueue {
			listener.onStateChanged(id, state)
		}
		val subscription = StateDisposableSubscription(this, listener)
		subscriptionContainer.add(subscription)
		return subscription
	}

	final override fun removeStateListener(listener: Session.StateListener<F>) {
		stateListeners -= listener
	}

	final override fun notifyCommitted() {
		val shouldNotifyCommitted = setCommitted()
		if (shouldNotifyCommitted) {
			onCommitted()
			notifyStateListeners(Committed)
		}
		executor.execute {
			sessionDao.updateLastCommitTimestamp(id.toString(), System.currentTimeMillis())
		}
	}

	final override fun complete(state: Completed<F>) {
		if (onCompleted(state)) {
			this.state = state
		}
	}

	final override fun completeExceptionally(exception: Exception) {
		state = Failed(exceptionalFailureFactory(exception))
	}

	final override fun cleanup() {
		cancellationSignal.setOnCancelListener(null)
		doCleanup()
		context.getSystemService<NotificationManager>()?.cancel(id.toString(), notificationId)
	}

	private fun notifyStateListeners(value: Session.State<F>) {
		persistSessionState(value)
		for (listener in stateListeners) {
			handler.post {
				listener.onStateChanged(id, value)
			}
		}
	}

	private fun tryEnterLaunch(): Boolean {
		val shouldNotify = updateState { current ->
			if (isCancelling.get() || current.isPreparing) {
				return false
			}
			if (current.state !is Pending && current.state !is Active) {
				return false
			}
			StateUpdate(
				newState = current.copy(state = Active, isPreparing = true),
				result = current.state !is Active
			)
		}
		if (shouldNotify) {
			notifyStateListeners(Active)
		}
		return true
	}

	private fun canPrepare() = updateState { current ->
		if (!current.isPreparing) {
			return false
		}
		if (!isCancelling.get() && (current.state is Pending || current.state is Active)) {
			return true
		}
		StateUpdate(
			newState = current.copy(isPreparing = false),
			result = false
		)
	}

	private fun tryEnterCommit() = updateState { current ->
		if (isCancelling.get() || current.isCommitCalled) {
			return false
		}
		if (current.state !is Awaiting && current.state !is Committed) {
			return false
		}
		StateUpdate(
			newState = current.copy(isCommitCalled = true),
			result = true
		)
	}

	private fun canCommit() = updateState { current ->
		if (!current.isCommitCalled) {
			return false
		}
		if (!isCancelling.get() && (current.state is Awaiting || current.state is Committed)) {
			return true
		}
		StateUpdate(
			newState = current.copy(isCommitCalled = false),
			result = false
		)
	}

	private fun setCommitted() = updateState { current ->
		val updatedState = if (current.state.isTerminal) current.state else Committed
		StateUpdate(
			newState = current.copy(state = updatedState, isCommitCalled = true),
			result = current.state !is Committed && !current.state.isTerminal
		)
	}

	private inline fun <R> updateState(block: (current: StateSnapshot<F>) -> StateUpdate<R, F>): R {
		while (true) {
			val current = stateSnapshot.get()
			val update = block(current)
			if (update.newState == current || stateSnapshot.compareAndSet(current, update.newState)) {
				return update.result
			}
		}
	}

	private fun persistSessionState(state: Session.State<F>) = executor.execute {
		dbWriteSemaphore.withPermit {
			when (state) {
				is Failed -> sessionFailureDao.setFailure(id.toString(), state.failure)
				else -> sessionDao.updateSessionState(id.toString(), state.toSessionEntityState())
			}
		}
		if (state.isTerminal) {
			cleanup()
		}
	}

	private fun Session.State<F>.toSessionEntityState() = when (this) {
		Pending -> SessionEntity.State.PENDING
		Active -> SessionEntity.State.ACTIVE
		Awaiting -> SessionEntity.State.AWAITING
		Committed -> SessionEntity.State.COMMITTED
		Cancelled -> SessionEntity.State.CANCELLED
		Succeeded -> SessionEntity.State.SUCCEEDED
		is Failed -> SessionEntity.State.FAILED
	}
}

private data class StateSnapshot<F : Failure>(
	val state: Session.State<F>,
	val isPreparing: Boolean,
	val isCommitCalled: Boolean
)

private class StateUpdate<out R, F : Failure>(
	val newState: StateSnapshot<F>,
	val result: R
)

private class StateDisposableSubscription<F : Failure>(
	session: Session<F>,
	listener: Session.StateListener<F>
) : DisposableSubscription {

	private val session = WeakReference(session)
	private val listener = WeakReference(listener)

	override var isDisposed: Boolean = false
		private set

	override fun dispose() {
		if (isDisposed) {
			return
		}
		val listener = this.listener.get()
		if (listener != null) {
			session.get()?.removeStateListener(listener)
		}
		this.listener.clear()
		session.clear()
		isDisposed = true
	}
}