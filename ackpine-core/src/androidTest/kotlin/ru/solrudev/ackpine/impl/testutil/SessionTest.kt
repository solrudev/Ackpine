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

package ru.solrudev.ackpine.impl.testutil

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import ru.solrudev.ackpine.remote.RemoteSession
import ru.solrudev.ackpine.remote.await
import ru.solrudev.ackpine.session.Failure
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.session.await
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

context(scope: CoroutineScope)
suspend fun <F : Failure> Session<F>.test(
	timeout: Duration = 30.seconds,
	block: suspend (Session<F>) -> Unit
): Session.State.Completed<F> {
	val resultDeferred = scope.async(start = UNDISPATCHED) {
		withContext(Dispatchers.Default.limitedParallelism(1)) {
			withTimeout(timeout) {
				await()
			}
		}
	}
	block(this)
	return resultDeferred.await()
}

context(scope: CoroutineScope)
suspend fun RemoteSession.test(
	timeout: Duration = 30.seconds,
	block: suspend (RemoteSession) -> Unit = {}
): RemoteSession.State {
	val resultDeferred = scope.async(start = UNDISPATCHED) {
		awaitWithTimeout(timeout)
	}
	block(this)
	return resultDeferred.await()
}

suspend fun RemoteSession.awaitWithTimeout(timeout: Duration = 30.seconds): RemoteSession.State {
	return withContext(Dispatchers.Default.limitedParallelism(1)) {
		withTimeout(timeout) {
			await()
		}
	}
}