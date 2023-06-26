package ru.solrudev.ackpine.impl.session

import android.os.Handler
import ru.solrudev.ackpine.DisposableSubscription
import ru.solrudev.ackpine.impl.database.dao.SessionDao
import ru.solrudev.ackpine.impl.database.dao.SessionFailureDao
import ru.solrudev.ackpine.impl.database.model.SessionEntity
import ru.solrudev.ackpine.session.Failure
import ru.solrudev.ackpine.session.Session
import java.util.UUID
import java.util.concurrent.Executor

internal abstract class AbstractSession<F : Failure> internal constructor(
	override val id: UUID,
	initialState: Session.State<F>,
	private val sessionDao: SessionDao,
	private val sessionFailureDao: SessionFailureDao<F>,
	private val executor: Executor,
	private val handler: Handler
) : Session<F> {

	private val stateListeners = mutableListOf<Session.StateListener<F>>()

	@Volatile
	protected var state = initialState
		set(value) {
			if (field == value) {
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

	override fun launch() {
		if (state !is Session.State.Pending) {
			return
		}
		executor.execute(::doLaunch)
		state = Session.State.Active
	}

	override fun commit() {
		if (state !is Session.State.Awaiting) {
			return
		}
		executor.execute(::doCommit)
		state = Session.State.Committed
	}

	override fun cancel() {
		if (state.isTerminal) {
			return
		}
		executor.execute(::doCancel)
		state = Session.State.Cancelled
	}

	override fun removeStateListener(listener: Session.StateListener<F>) {
		stateListeners -= listener
	}

	override fun addStateListener(listener: Session.StateListener<F>): DisposableSubscription {
		stateListeners += listener
		handler.post {
			listener.onStateChanged(id, state)
		}
		return StateDisposableSubscription(this, listener)
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