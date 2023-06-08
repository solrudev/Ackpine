package ru.solrudev.ackpine

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.emptyFlow
import java.util.UUID

public fun PackageInstaller.getProgressFlow(sessionId: UUID): Flow<Progress> {
	val session = getSession(sessionId) ?: return emptyFlow()
	return callbackFlow {
		val subscriptionContainer = DisposableSubscriptionContainer()
		subscriptionContainer += addProgressListener(sessionId) { _, progress ->
			trySend(progress)
		}
		subscriptionContainer += session.addStateListener { _, state ->
			if (state.isTerminal) {
				channel.close()
			}
		}
		awaitClose(subscriptionContainer::dispose)
	}.conflate()
}