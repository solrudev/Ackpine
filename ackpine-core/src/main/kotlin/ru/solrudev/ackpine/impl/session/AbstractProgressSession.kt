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
import java.lang.ref.WeakReference
import java.util.UUID
import java.util.concurrent.Executor

/**
 * A base implementation for Ackpine [sessions with progress][ProgressSession].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal abstract class AbstractProgressSession<F : Failure> protected constructor(
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

	private val progressListeners = mutableSetOf<ProgressSession.ProgressListener>()

	@Volatile
	protected var progress = initialProgress
		set(value) {
			if (field == value) {
				return
			}
			field = value
			persistSessionProgress(value)
			for (listener in progressListeners) {
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
	session: ProgressSession<*>,
	listener: ProgressSession.ProgressListener
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
			session.get()?.removeProgressListener(listener)
		}
		this.listener.clear()
		session.clear()
		isDisposed = true
	}
}