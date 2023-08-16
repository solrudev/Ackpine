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
import ru.solrudev.ackpine.impl.database.dao.NotificationIdDao
import ru.solrudev.ackpine.impl.database.dao.SessionDao
import ru.solrudev.ackpine.impl.database.dao.SessionFailureDao
import ru.solrudev.ackpine.impl.database.model.SessionEntity
import ru.solrudev.ackpine.impl.session.helpers.launchConfirmation
import ru.solrudev.ackpine.session.Failure
import ru.solrudev.ackpine.session.Session
import java.lang.ref.WeakReference
import java.util.UUID
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

private val globalNotificationId = AtomicInteger(Random.nextInt(from = 10000, until = 1000000))

/**
 * A base implementation for Ackpine [sessions][Session].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal abstract class AbstractSession<F : Failure> internal constructor(
	private val context: Context,
	private val notificationTag: String,
	override val id: UUID,
	initialState: Session.State<F>,
	private val sessionDao: SessionDao,
	private val sessionFailureDao: SessionFailureDao<F>,
	private val notificationIdDao: NotificationIdDao,
	private val serialExecutor: Executor,
	private val handler: Handler,
	private val exceptionalFailureFactory: (Exception) -> F
) : CompletableSession<F> {

	init {
		serialExecutor.execute {
			notificationId = notificationIdDao.getNotificationId(id.toString()).takeIf { it != -1 }
				?: globalNotificationId.incrementAndGet().also { notificationId ->
					notificationIdDao.setNotificationId(id.toString(), notificationId)
				}
		}
	}

	private var notificationId = 0
	private val stateListeners = mutableSetOf<Session.StateListener<F>>()
	private val cancellationSignal = CancellationSignal()

	@Volatile
	private var isCancelling = false

	@Volatile
	private var isPreparing = false

	private val stateLock = Any()

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
			persistSessionState(value)
			stateListeners.forEach { listener ->
				handler.post {
					listener.onStateChanged(id, value)
				}
			}
		}

	final override val isActive: Boolean
		get() = state.let { it !is Session.State.Pending && !it.isTerminal }

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
	protected abstract fun launchConfirmation(cancellationSignal: CancellationSignal, notificationId: Int)

	/**
	 * Release any held resources after session's completion or cancellation. This method is called on a worker thread.
	 */
	@WorkerThread
	protected open fun doCleanup() {}

	/**
	 * Implementation allows to re-launch the session when it's not in process of preparations and session's state
	 * hasn't reached [Session.State.Awaiting] yet, e.g. when preparations were interrupted with process death.
	 */
	final override fun launch() {
		if (isPreparing || isCancelling) {
			return
		}
		val currentState = state
		if (currentState !is Session.State.Pending && currentState !is Session.State.Active) {
			return
		}
		isPreparing = true
		state = Session.State.Active
		serialExecutor.execute {
			try {
				sessionDao.updateLastLaunchTimestamp(id.toString(), System.currentTimeMillis())
				prepare(cancellationSignal)
			} catch (_: OperationCanceledException) {
				handleCancellation()
			} catch (exception: Exception) {
				handleException(exception)
			}
		}
	}

	final override fun commit() {
		if (state !is Session.State.Awaiting || isCancelling) {
			return
		}
		serialExecutor.execute {
			try {
				launchConfirmation(cancellationSignal, notificationId)
			} catch (_: OperationCanceledException) {
				handleCancellation()
			} catch (exception: Exception) {
				handleException(exception)
			}
		}
	}

	final override fun cancel() {
		if (state.isTerminal || isCancelling) {
			return
		}
		isCancelling = true
		serialExecutor.execute {
			try {
				cancellationSignal.cancel()
				handleCancellation()
			} catch (exception: Exception) {
				handleException(exception)
			} finally {
				isCancelling = false
			}
		}
	}

	final override fun addStateListener(listener: Session.StateListener<F>): DisposableSubscription {
		stateListeners += listener
		handler.post {
			listener.onStateChanged(id, state)
		}
		return StateDisposableSubscription(this, listener)
	}

	final override fun removeStateListener(listener: Session.StateListener<F>) {
		stateListeners -= listener
	}

	final override fun notifyCommitted() {
		onCommitted()
		state = Session.State.Committed
	}

	final override fun complete(state: Session.State.Completed<F>) {
		onCompleted(state is Session.State.Succeeded)
		this.state = state
		serialExecutor.execute {
			cleanup()
		}
	}

	final override fun completeExceptionally(exception: Exception) = serialExecutor.execute {
		handleException(exception)
	}

	/**
	 * Notifies that preparations are done and sets session's state to [Session.State.Awaiting].
	 */
	protected fun notifyAwaiting() {
		isPreparing = false
		state = Session.State.Awaiting
	}

	/**
	 * This callback method is invoked when the session's been committed. Processing in this method should be
	 * lightweight.
	 */
	protected open fun onCommitted() {}

	/**
	 * This callback method is invoked when the session's been [completed][Session.State.isCompleted]. Processing in
	 * this method should be lightweight.
	 */
	protected open fun onCompleted(success: Boolean) {}

	private fun cleanup() {
		doCleanup()
		context.getSystemService<NotificationManager>()?.cancel(notificationTag, notificationId)
	}

	private fun handleCancellation() {
		state = Session.State.Cancelled
		cleanup()
	}

	private fun handleException(exception: Exception) {
		state = Session.State.Failed(exceptionalFailureFactory(exception))
		cleanup()
	}

	private fun persistSessionState(value: Session.State<F>) = serialExecutor.execute {
		when (value) {
			is Session.State.Failed -> sessionFailureDao.setFailure(id.toString(), value.failure)
			else -> sessionDao.updateSessionState(id.toString(), value.toSessionEntityState())
		}
	}

	private fun Session.State<F>.toSessionEntityState() = when (this) {
		Session.State.Pending -> SessionEntity.State.PENDING
		Session.State.Active -> SessionEntity.State.ACTIVE
		Session.State.Awaiting -> SessionEntity.State.AWAITING
		Session.State.Committed -> SessionEntity.State.COMMITTED
		Session.State.Cancelled -> SessionEntity.State.CANCELLED
		Session.State.Succeeded -> SessionEntity.State.SUCCEEDED
		is Session.State.Failed -> SessionEntity.State.FAILED
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