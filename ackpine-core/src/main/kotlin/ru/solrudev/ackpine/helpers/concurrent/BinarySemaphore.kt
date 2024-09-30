/*
 * Copyright (C) 2024 Ilya Fomichev
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

package ru.solrudev.ackpine.helpers.concurrent

import androidx.annotation.RestrictTo
import java.util.concurrent.Semaphore

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class BinarySemaphore : Semaphore(1) {
	private companion object {
		private const val serialVersionUID: Long = -1496811586616841420L
	}
}

@JvmSynthetic
internal inline fun <T> BinarySemaphore.withPermit(action: () -> T): T {
	acquire()
	try {
		return action()
	} finally {
		release()
	}
}