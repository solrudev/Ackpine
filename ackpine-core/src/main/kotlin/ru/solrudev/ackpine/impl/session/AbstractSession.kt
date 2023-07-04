package ru.solrudev.ackpine.impl.session

import android.app.NotificationManager
import android.content.Context
import android.os.CancellationSignal
import android.os.Handler
import android.os.OperationCanceledException
import androidx.annotation.RestrictTo
import androidx.core.content.getSystemService
import ru.solrudev.ackpine.DisposableSubscription
import ru.solrudev.ackpine.impl.database.dao.NotificationIdDao
import ru.solrudev.ackpine.impl.database.dao.SessionDao
import ru.solrudev.ackpine.impl.database.dao.SessionFailureDao
import ru.solrudev.ackpine.impl.database.model.SessionEntity
import ru.solrudev.ackpine.session.Failure
import ru.solrudev.ackpine.session.Session
import java.util.UUID
import java.util.concurrent.CancellationException
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

private val NOTIFICATION_ID = AtomicInteger(Random.nextInt(from = 10000, until = 1000000))

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
			notificationId = notificationIdDao.getNotificationId(id.toString())
				?: NOTIFICATION_ID.incrementAndGet().also { notificationId ->
					notificationIdDao.setNotificationId(id.toString(), notificationId)
				}
		}
	}

	private val stateListeners = mutableListOf<Session.StateListener<F>>()
	private var notificationId = 0
	private val cancellationSignal = CancellationSignal()

	@Volatile
	private var isCancelling = false

	@Volatile
	private var isPreparing = false

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

	final override val isActive: Boolean
		get() = state.let { it !is Session.State.Pending && !it.isTerminal }

	protected abstract fun doLaunch(cancellationSignal: CancellationSignal)
	protected abstract fun doCommit(cancellationSignal: CancellationSignal)
	protected open fun doCleanup() {}

	final override fun launch() {
		if (isPreparing || isCancelling) {
			return
		}
		val currentState = state
		if (currentState !is Session.State.Pending && currentState !is Session.State.Active) {
			return
		}
		state = Session.State.Active
		serialExecutor.execute {
			try {
				isPreparing = true
				doLaunch(cancellationSignal)
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

	final override fun commit() {
		if (state !is Session.State.Awaiting || isCancelling) {
			return
		}
		serialExecutor.execute {
			try {
				doCommit(cancellationSignal)
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

	final override fun cancel() {
		if (state.isTerminal || isCancelling) {
			return
		}
		serialExecutor.execute {
			try {
				isCancelling = true
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
		state = Session.State.Committed
	}

	final override fun complete(state: Session.State.Completed<F>) {
		this.state = state
		serialExecutor.execute {
			cleanup()
		}
	}

	final override fun completeExceptionally(exception: Exception) = serialExecutor.execute {
		handleException(exception)
	}

	protected fun notifyAwaiting() {
		isPreparing = false
		state = Session.State.Awaiting
	}

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