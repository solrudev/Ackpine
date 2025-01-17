/*
 * Copyright (C) 2025 Ilya Fomichev
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

package ru.solrudev.ackpine.splits

import androidx.concurrent.futures.await
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job

/**
 * A suspending variant of [getAsync][SplitPackage.Provider.getAsync].
 *
 * This suspending function is cancellable.
 * If the [Job] of the current coroutine is cancelled or completed while this suspending function is waiting, this
 * function immediately resumes with [CancellationException].
 */
public suspend fun SplitPackage.Provider.get(): SplitPackage {
	return getAsync().await()
}

/**
 * A suspending variant of [toListAsync][SplitPackage.Provider.toListAsync].
 *
 * This suspending function is cancellable.
 * If the [Job] of the current coroutine is cancelled or completed while this suspending function is waiting, this
 * function immediately resumes with [CancellationException].
 */
public suspend fun SplitPackage.Provider.toList(): List<SplitPackage.Entry<*>> {
	return toListAsync().await()
}