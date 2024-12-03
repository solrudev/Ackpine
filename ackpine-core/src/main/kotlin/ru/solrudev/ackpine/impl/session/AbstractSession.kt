/*
 * Copyright (C) 2023-2024 Ilya Fomichev
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
import ru.solrudev.ackpine.helpers.concurrent.BinarySemaphore
import ru.solrudev.ackpine.helpers.concurrent.withPermit
import ru.solrudev.ackpine.impl.database.dao.SessionDao
import ru.solrudev.ackpine.impl.database.dao.SessionFailureDao
import ru.solrudev.ackpine.impl.database.model.SessionEntity
import ru.solrudev.ackpine.impl.session.helpers.launchConfirmation
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
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater

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
	private val notificationId: Int,
	private val dbWriteSemaphore: BinarySemaphore
) : CompletableSession<F> {

	private val stateListeners = Collections.newSetFromMap(
		ConcurrentHashMap<Session.StateListener<F>, Boolean>()
	)

	private val cancellationSignal = CancellationSignal()
	private val stateLock = Any()
	private val isCancelling = AtomicBoolean(false)
	private val isCommitCalled = AtomicBoolean(false)

	@Volatile
	private var isPreparing = false

	@Volatile
	private var state = initialState
		set(value) {
			synchronized(stateLock) {
				val currentValue = field
				if (currentValue == value || currentValue.isTerminal) {
					return
				}
				field = value
			}
			notifyStateListeners(value)
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
	protected abstract fun prepare(cancellationSignal: CancellationSignal)

	/**
	 * Launch session's confirmation with [Context.launchConfirmation]. This method is called on a worker thread.
	 */
	@WorkerThread
	protected abstract fun launchConfirmation(notificationId: Int)

	/**
	 * Release any held resources after session's completion or cancellation. Processing in this method should be
	 * lightweight.
	 */
	protected open fun doCleanup() { /* optional */ }

	/**
	 * Notifies that preparations are done and sets session's state to [Awaiting].
	 */
	protected fun notifyAwaiting() {
		state = Awaiting
		isPreparing = false
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
		if (isPreparing || isCancelling.get()) {
			return false
		}
		val isStateChangedToActive = stateUpdater.compareAndSet(this, Pending, Active)
		if (isStateChangedToActive) {
			notifyStateListeners(Active)
		}
		if (!isStateChangedToActive || state !is Active) {
			return false
		}
		isPreparing = true
		executor.execute {
			try {
				sessionDao.updateLastLaunchTimestamp(id.toString(), System.currentTimeMillis())
				prepare(cancellationSignal)
			} catch (_: OperationCanceledException) {
				handleCancellation()
			} catch (exception: Exception) {
				completeExceptionally(exception)
			}
		}
		return true
	}

	final override fun commit(): Boolean {
		if (isCancelling.get()) {
			return false
		}
		val currentState = state
		if (currentState !is Awaiting && currentState !is Committed) {
			return false
		}
		if (!isCommitCalled.compareAndSet(false, true)) {
			return false
		}
		executor.execute {
			try {
				launchConfirmation(notificationId)
			} catch (_: OperationCanceledException) {
				handleCancellation()
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
			handleCancellation()
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
		isCommitCalled.set(true)
		if (state !is Committed) {
			onCommitted()
			state = Committed
		}
		executor.execute {
			sessionDao.updateLastCommitTimestamp(id.toString(), System.currentTimeMillis())
		}
	}

	final override fun complete(state: Completed<F>) {
		if (onCompleted(state)) {
			this.state = state
			cleanup()
		}
	}

	final override fun completeExceptionally(exception: Exception) {
		state = Failed(exceptionalFailureFactory(exception))
		cleanup()
	}

	private fun notifyStateListeners(value: Session.State<F>) {
		persistSessionState(value)
		for (listener in stateListeners) {
			handler.post {
				listener.onStateChanged(id, value)
			}
		}
	}

	private fun cleanup() {
		doCleanup()
		context.getSystemService<NotificationManager>()?.cancel(id.toString(), notificationId)
	}

	private fun handleCancellation() {
		state = Cancelled
		cleanup()
	}

	private fun persistSessionState(value: Session.State<F>) = executor.execute {
		dbWriteSemaphore.withPermit {
			when (value) {
				is Failed -> sessionFailureDao.setFailure(id.toString(), value.failure)
				else -> sessionDao.updateSessionState(id.toString(), value.toSessionEntityState())
			}
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

	private companion object {
		private val stateUpdater = AtomicReferenceFieldUpdater.newUpdater(
			AbstractSession::class.java, Session.State::class.java, "state"
		)
	}
}

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