package ru.solrudev.ackpine.session

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.first

/**
 * Returns a cold [Flow] of [session's][Session] [state][Session.State]. It will emit current state immediately when
 * collecting is triggered.
 *
 * Use [state.first()][Flow.first] to access current state without collecting it.
 *
 * This flow completes when the session's state is [terminal][Session.State.isTerminal].
 */
public val <F : Failure> Session<F>.state: Flow<Session.State<F>>
	get() = callbackFlow {
		val subscription = addStateListener { _, state ->
			trySend(state)
			if (state.isTerminal) {
				channel.close()
			}
		}
		awaitClose(subscription::dispose)
	}.conflate()