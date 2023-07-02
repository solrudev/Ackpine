package ru.solrudev.ackpine.impl.session

import android.os.Handler
import android.os.OperationCanceledException
import ru.solrudev.ackpine.DisposableSubscription
import ru.solrudev.ackpine.impl.database.dao.SessionDao
import ru.solrudev.ackpine.impl.database.dao.SessionFailureDao
import ru.solrudev.ackpine.impl.database.model.SessionEntity
import ru.solrudev.ackpine.session.Failure
import ru.solrudev.ackpine.session.Session
import java.util.UUID
import java.util.concurrent.CancellationException
import java.util.concurrent.Executor

internal abstract class AbstractSession<F : Failure> internal constructor(
	override val id: UUID,
	initialState: Session.State<F>,
	private val sessionDao: SessionDao,
	private val sessionFailureDao: SessionFailureDao<F>,
	private val executor: Executor,
	private val handler: Handler,
	private val exceptionalFailureFactory: (Exception) -> F
) : CompletableSession<F> {

	private val stateListeners = mutableListOf<Session.StateListener<F>>()

	@Volatile
	private var isCancelling = false

	@Volatile
	private var state = initialState
		set(value) {
			val currentValue = field
			if (currentValue == value || currentValue.isTerminal) {
				return
			}
			field = value
			persistSessionState(value)
			stateListeners.forEach { listener ->
				handler.post {
					listener.onStateChanged(id, value)
				}
			}
		}

	override val isActive: Boolean
		get() = with(state) { this !is Session.State.Pending && !isTerminal }

	protected abstract fun doLaunch()
	protected abstract fun doCommit()
	protected abstract fun doCancel()
	protected abstract fun cleanup()

	override fun launch() {
		if (state !is Session.State.Pending || isCancelling) {
			return
		}
		state = Session.State.Active
		executor.execute {
			try {
				doLaunch()
			} catch (_: OperationCanceledException) {
				handleCancellation()
			} catch (_: CancellationException) {
				handleCancellation()
			} catch (_: InterruptedException) {
				handleCancellation()
			} catch (exception: Exception) {
				handleException(exception)
			}
		}
	}

	override fun commit() {
		if (state !is Session.State.Awaiting || isCancelling) {
			return
		}
		executor.execute {
			try {
				doCommit()
			} catch (_: OperationCanceledException) {
				handleCancellation()
			} catch (_: CancellationException) {
				handleCancellation()
			} catch (_: InterruptedException) {
				handleCancellation()
			} catch (exception: Exception) {
				handleException(exception)
			}
		}
	}

	override fun cancel() {
		if (state.isTerminal || isCancelling) {
			return
		}
		executor.execute {
			try {
				isCancelling = true
				doCancel()
				handleCancellation()
			} catch (exception: Exception) {
				handleException(exception)
			} finally {
				isCancelling = false
			}
		}
	}

	override fun addStateListener(listener: Session.StateListener<F>): DisposableSubscription {
		stateListeners += listener
		handler.post {
			listener.onStateChanged(id, state)
		}
		return StateDisposableSubscription(this, listener)
	}

	override fun removeStateListener(listener: Session.StateListener<F>) {
		stateListeners -= listener
	}

	override fun notifyCommitted() {
		state = Session.State.Committed
	}

	override fun complete(state: Session.State.Completed<F>) {
		this.state = state
		executor.execute {
			cleanup()
		}
	}

	override fun completeExceptionally(exception: Exception) = executor.execute {
		handleException(exception)
	}

	protected fun notifyAwaiting() {
		state = Session.State.Awaiting
	}

	private fun handleCancellation() {
		state = Session.State.Cancelled
		cleanup()
	}

	private fun handleException(exception: Exception) {
		state = Session.State.Failed(exceptionalFailureFactory(exception))
		cleanup()
	}

	private fun persistSessionState(value: Session.State<F>) = executor.execute {
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
	private var session: Session<F>?,
	private var listener: Session.StateListener<F>?
) : DisposableSubscription {

	override var isDisposed: Boolean = false
		private set

	override fun dispose() {
		if (isDisposed) {
			return
		}
		val listener = this.listener
		if (listener != null) {
			session?.removeStateListener(listener)
		}
		this.listener = null
		session = null
		isDisposed = true
	}
}