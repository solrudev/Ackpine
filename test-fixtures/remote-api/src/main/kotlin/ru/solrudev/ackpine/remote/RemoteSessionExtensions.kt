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

package ru.solrudev.ackpine.remote

import android.os.RemoteException
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import ru.solrudev.ackpine.DisposableSubscriptionContainer
import ru.solrudev.ackpine.session.Session.TerminalStateListener
import java.util.UUID
import kotlin.coroutines.resume

/**
 * Returns a cold flow of [RemoteSession.State] for the remote session. It will emit current state immediately when
 * collecting is triggered.
 *
 * Use [state.first()][kotlinx.coroutines.flow.first] to access current state without collecting it.
 *
 * This flow completes when the session's state is terminal.
 */
public val RemoteSession.state: Flow<RemoteSession.State>
	get() = callbackFlow {
		val subscriptionContainer = DisposableSubscriptionContainer()
		addStateListener(subscriptionContainer) { _, state ->
			trySend(state)
			if (state.isTerminal) {
				channel.close()
			}
		}
		awaitClose {
			try {
				subscriptionContainer.dispose()
			} catch (_: RemoteException) { // ignore
			}
		}
	}

/**
 * Launches the [RemoteSession] if it's not already, awaits for its completion without blocking a thread and resumes
 * when it's complete, returning the resulting value or throwing the corresponding exception if the session was
 * cancelled.
 *
 * This suspending function is cancellable.
 * If the [Job] of the current coroutine is cancelled or completed while this suspending function is waiting, this
 * function immediately resumes with [CancellationException] and cancels the session.
 *
 * This function handles session's lifecycle appropriately, like [TerminalStateListener].
 *
 * @return [RemoteSession.State]
 */
public suspend fun RemoteSession.await(): RemoteSession.State {
	return suspendCancellableCoroutine { continuation ->
		val subscriptionContainer = DisposableSubscriptionContainer()
		addStateListener(subscriptionContainer, AwaitRemoteSessionStateListener(this, continuation))
		continuation.invokeOnCancellation {
			try {
				subscriptionContainer.dispose()
				cancel()
			} catch (_: RemoteException) { // ignore
			}
		}
	}
}

private class AwaitRemoteSessionStateListener(
	private val session: RemoteSession,
	private val continuation: CancellableContinuation<RemoteSession.State>
) : RemoteSession.StateListener {

	override fun onStateChanged(sessionId: UUID, state: RemoteSession.State) {
		try {
			if (state.isTerminal) {
				session.removeStateListener(this)
			}
			when (state) {
				RemoteSession.State.Pending -> session.launch()
				RemoteSession.State.Active -> session.launch() // re-launch if preparations were interrupted
				RemoteSession.State.Awaiting -> session.commit()
				RemoteSession.State.Committed -> session.commit() // re-commit if confirmation was interrupted
				RemoteSession.State.Cancelled -> continuation.cancel()
				is RemoteSession.State.Failed, RemoteSession.State.Succeeded -> continuation.resume(state)
			}
		} catch (_: RemoteException) {
			continuation.cancel()
		}
	}
}