package ru.solrudev.ackpine.session

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.first
import ru.solrudev.ackpine.DisposableSubscriptionContainer
import ru.solrudev.ackpine.plusAssign

/**
 * Returns a cold [Flow] of [session's][ProgressSession] [progress][Progress]. It will emit current progress immediately
 * when collecting is triggered.
 *
 * Use [progress.first()][Flow.first] to access current progress without collecting it.
 *
 * This flow completes when the session's state is [terminal][Session.State.isTerminal].
 */
public val ProgressSession<*>.progress: Flow<Progress>
	get() = callbackFlow {
		val subscriptionContainer = DisposableSubscriptionContainer()
		subscriptionContainer += addProgressListener { _, progress ->
			trySend(progress)
		}
		subscriptionContainer += addStateListener { _, state ->
			if (state.isTerminal) {
				channel.close()
			}
		}
		awaitClose(subscriptionContainer::dispose)
	}.conflate()