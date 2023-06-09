package ru.solrudev.ackpine.session

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

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