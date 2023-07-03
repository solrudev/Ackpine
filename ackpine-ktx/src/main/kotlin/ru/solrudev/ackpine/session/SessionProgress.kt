package ru.solrudev.ackpine.session

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import ru.solrudev.ackpine.DisposableSubscriptionContainer
import ru.solrudev.ackpine.plusAssign

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