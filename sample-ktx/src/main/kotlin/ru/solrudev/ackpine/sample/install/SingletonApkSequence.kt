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

package ru.solrudev.ackpine.sample.install

import android.content.Context
import android.net.Uri
import android.os.CancellationSignal
import ru.solrudev.ackpine.splits.Apk
import ru.solrudev.ackpine.splits.CloseableSequence
import kotlin.concurrent.Volatile

class SingletonApkSequence(private val uri: Uri, context: Context) : CloseableSequence<Apk> {

	@Volatile
	override var isClosed: Boolean = false
		private set

	private val applicationContext = context.applicationContext
	private val cancellationSignal = CancellationSignal()

	override fun iterator(): Iterator<Apk> {
		return object : Iterator<Apk> {

			private val apk = Apk.fromUri(uri, applicationContext, cancellationSignal)
			private var isYielded = false

			override fun hasNext(): Boolean {
				return apk != null && !isYielded
			}

			override fun next(): Apk {
				if (!hasNext()) {
					throw NoSuchElementException()
				}
				isYielded = true
				return apk!!
			}
		}
	}

	override fun close() {
		isClosed = true
		cancellationSignal.cancel()
	}
}