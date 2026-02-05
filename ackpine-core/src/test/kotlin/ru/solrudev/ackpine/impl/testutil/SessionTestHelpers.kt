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

import ru.solrudev.ackpine.DisposableSubscriptionContainer
import ru.solrudev.ackpine.session.Failure
import ru.solrudev.ackpine.session.Progress
import ru.solrudev.ackpine.session.ProgressSession
import ru.solrudev.ackpine.session.Session

internal fun <F : Failure> Session<F>.captureStates(): List<Session.State<F>> {
	val states = mutableListOf<Session.State<F>>()
	addStateListener(DisposableSubscriptionContainer()) { _, state -> states += state }
	idleMainThread()
	return states
}

internal fun <F : Failure> ProgressSession<F>.captureProgress(): List<Progress> {
	val progress = mutableListOf<Progress>()
	addProgressListener(DisposableSubscriptionContainer()) { _, p -> progress += p }
	idleMainThread()
	return progress
}