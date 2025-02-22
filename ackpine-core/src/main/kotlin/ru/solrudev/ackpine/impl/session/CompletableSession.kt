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

import androidx.annotation.RestrictTo
import ru.solrudev.ackpine.session.Failure
import ru.solrudev.ackpine.session.ProgressSession
import ru.solrudev.ackpine.session.Session

/**
 * A [Session] which can be completed.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal interface CompletableSession<F : Failure> : Session<F> {

	/**
	 * Complete the session normally with a [completed state][Session.State.Completed] value.
	 */
	fun complete(state: Session.State.Completed<F>)

	/**
	 * Complete the session with an [exception].
	 */
	fun completeExceptionally(exception: Exception)

	/**
	 * Notify that the session's been committed.
	 */
	fun notifyCommitted()
}

/**
 * A [ProgressSession] which can be completed.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal interface CompletableProgressSession<F : Failure> : CompletableSession<F>, ProgressSession<F>