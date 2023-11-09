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

package ru.solrudev.ackpine.session

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.first
import ru.solrudev.ackpine.DisposableSubscriptionContainer

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
		val subscriptionContainer = DisposableSubscriptionContainer()
		addStateListener(subscriptionContainer) { _, state ->
			trySend(state)
			if (state.isTerminal) {
				channel.close()
			}
		}
		awaitClose(subscriptionContainer::dispose)
	}.conflate()