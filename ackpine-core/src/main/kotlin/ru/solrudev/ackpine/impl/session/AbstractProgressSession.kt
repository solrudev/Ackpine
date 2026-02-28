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
import ru.solrudev.ackpine.DisposableSubscriptionContainer
import ru.solrudev.ackpine.DummyDisposableSubscription
import ru.solrudev.ackpine.impl.database.dao.SessionDao
import ru.solrudev.ackpine.impl.database.dao.SessionFailureDao
import ru.solrudev.ackpine.impl.database.dao.SessionProgressDao
import ru.solrudev.ackpine.impl.helpers.concurrent.BinarySemaphore
import ru.solrudev.ackpine.impl.helpers.concurrent.SerialExecutor
import ru.solrudev.ackpine.impl.installer.session.helpers.PROGRESS_MAX
import ru.solrudev.ackpine.session.Failure
import ru.solrudev.ackpine.session.Progress
import ru.solrudev.ackpine.session.ProgressSession
import ru.solrudev.ackpine.session.Session
import java.util.UUID
import java.util.concurrent.Executor

/**
 * A base implementation for Ackpine [sessions with progress][ProgressSession].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal abstract class AbstractProgressSession<F : Failure> protected constructor(
	context: Context,
	id: UUID,
	initialState: Session.State<F>,
	initialProgress: Progress,
	sessionDao: SessionDao,
	sessionFailureDao: SessionFailureDao<F>,
	private val sessionProgressDao: SessionProgressDao,
	executor: Executor,
	private val handler: Handler,
	exceptionalFailureFactory: (Exception) -> F,
	notificationId: Int,
	dbWriteSemaphore: BinarySemaphore
) : AbstractSession<F>(
	context, id, initialState,
	sessionDao, sessionFailureDao,
	executor, handler, exceptionalFailureFactory, notificationId, dbWriteSemaphore
), CompletableProgressSession<F> {

	private val serialExecutor = SerialExecutor(executor)
	private val progressListeners = ListenerStore<ProgressSession.ProgressListener>()

	@Volatile
	private var progress = initialProgress
		set(value) {
			if (field == value) {
				return
			}
			field = value
			persistSessionProgress(value)
			progressListeners.forEach { registration ->
				handler.post {
					if (progressListeners.isValid(registration)) {
						registration.listener.onProgressChanged(id, value)
					}
				}
			}
		}

	final override fun addProgressListener(
		subscriptionContainer: DisposableSubscriptionContainer,
		listener: ProgressSession.ProgressListener
	): DisposableSubscription {
		val registration = progressListeners.add(listener) ?: return DummyDisposableSubscription
		handler.postAtFrontOfQueue {
			if (progressListeners.isValid(registration)) {
				listener.onProgressChanged(id, progress)
			}
		}
		val subscription = progressListeners.subscriptionOf(registration)
		subscriptionContainer.add(subscription)
		return subscription
	}

	final override fun removeProgressListener(listener: ProgressSession.ProgressListener) {
		progressListeners.remove(listener)
	}

	protected fun setProgress(value: Int) {
		progress = Progress(value, PROGRESS_MAX)
	}

	private fun persistSessionProgress(value: Progress) = serialExecutor.execute {
		sessionProgressDao.updateProgress(id.toString(), value.progress, value.max)
	}
}