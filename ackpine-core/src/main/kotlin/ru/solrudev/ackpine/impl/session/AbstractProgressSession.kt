package ru.solrudev.ackpine.impl.session

import android.content.Context
import android.os.Handler
import androidx.annotation.RestrictTo
import ru.solrudev.ackpine.DisposableSubscription
import ru.solrudev.ackpine.impl.database.dao.NotificationIdDao
import ru.solrudev.ackpine.impl.database.dao.SessionDao
import ru.solrudev.ackpine.impl.database.dao.SessionFailureDao
import ru.solrudev.ackpine.impl.database.dao.SessionProgressDao
import ru.solrudev.ackpine.session.Failure
import ru.solrudev.ackpine.session.Progress
import ru.solrudev.ackpine.session.ProgressSession
import ru.solrudev.ackpine.session.Session
import java.util.UUID
import java.util.concurrent.Executor

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal abstract class AbstractProgressSession<F : Failure> internal constructor(
	context: Context,
	notificationTag: String,
	id: UUID,
	initialState: Session.State<F>,
	initialProgress: Progress,
	sessionDao: SessionDao,
	sessionFailureDao: SessionFailureDao<F>,
	private val sessionProgressDao: SessionProgressDao,
	notificationIdDao: NotificationIdDao,
	private val serialExecutor: Executor,
	private val handler: Handler,
	exceptionalFailureFactory: (Exception) -> F
) : AbstractSession<F>(
	context, notificationTag,
	id, initialState,
	sessionDao, sessionFailureDao, notificationIdDao,
	serialExecutor, handler, exceptionalFailureFactory
), ProgressSession<F> {

	private val progressListeners = mutableListOf<ProgressSession.ProgressListener>()

	@Volatile
	protected var progress = initialProgress
		set(value) {
			if (field == value) {
				return
			}
			field = value
			persistSessionProgress(value)
			progressListeners.forEach { listener ->
				handler.post {
					listener.onProgressChanged(id, value)
				}
			}
		}

	final override fun addProgressListener(listener: ProgressSession.ProgressListener): DisposableSubscription {
		progressListeners += listener
		handler.post {
			listener.onProgressChanged(id, progress)
		}
		return ProgressDisposableSubscription(this, listener)
	}

	final override fun removeProgressListener(listener: ProgressSession.ProgressListener) {
		progressListeners -= listener
	}

	private fun persistSessionProgress(value: Progress) = serialExecutor.execute {
		sessionProgressDao.updateProgress(id.toString(), value.progress, value.max)
	}
}

private class ProgressDisposableSubscription(
	private var session: ProgressSession<*>?,
	private var listener: ProgressSession.ProgressListener?
) : DisposableSubscription {

	override var isDisposed: Boolean = false
		private set

	override fun dispose() {
		if (isDisposed) {
			return
		}
		val listener = this.listener
		if (listener != null) {
			session?.removeProgressListener(listener)
		}
		this.listener = null
		session = null
		isDisposed = true
	}
}