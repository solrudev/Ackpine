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
		addProgressListener(subscriptionContainer) { _, progress ->
			trySend(progress)
		}
		addStateListener(subscriptionContainer) { _, state ->
			if (state.isTerminal) {
				channel.close()
			}
		}
		awaitClose(subscriptionContainer::dispose)
	}.conflate()