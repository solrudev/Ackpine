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

package ru.solrudev.ackpine.impl.installer

import android.content.Context
import android.content.Context.MODE_PRIVATE
import androidx.annotation.RestrictTo
import androidx.annotation.WorkerThread
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.core.content.edit
import com.google.common.util.concurrent.ListenableFuture
import ru.solrudev.ackpine.AckpineThreadPool

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal object CommitProgressValueHolder {

	private const val ACKPINE_SESSION_BASED_INSTALLER = "ackpine_session_based_installer"
	private const val SESSION_COMMIT_PROGRESS_VALUE = "session_commit_progress_value"

	@Volatile
	private var commitProgressValue = -1f

	@WorkerThread
	@JvmSynthetic
	internal fun putIfAbsent(context: Context, valueProducer: () -> Float) {
		val preferences = context.getSharedPreferences(ACKPINE_SESSION_BASED_INSTALLER, MODE_PRIVATE)
		if (!preferences.contains(SESSION_COMMIT_PROGRESS_VALUE)) {
			val value = valueProducer()
			commitProgressValue = value
			preferences.edit(commit = true) {
				putFloat(SESSION_COMMIT_PROGRESS_VALUE, value)
			}
		}
	}

	@WorkerThread
	@JvmSynthetic
	internal fun get(context: Context): Float {
		val cachedValue = commitProgressValue
		if (cachedValue < 0) {
			val value = getValue(context)
			commitProgressValue = value
			return value
		}
		return cachedValue
	}

	@JvmSynthetic
	internal fun getAsync(context: Context): ListenableFuture<Float> {
		val applicationContext = context.applicationContext
		return CallbackToFutureAdapter.getFuture { completer ->
			val cachedValue = commitProgressValue
			if (cachedValue < 0) {
				AckpineThreadPool.executor.execute {
					val value = getValue(applicationContext)
					commitProgressValue = value
					completer.set(value)
				}
			} else {
				completer.set(cachedValue)
			}
			"CommitProgressValueHolder.getAsync"
		}
	}

	private fun getValue(context: Context) = context
		.getSharedPreferences(ACKPINE_SESSION_BASED_INSTALLER, MODE_PRIVATE)
		.getFloat(SESSION_COMMIT_PROGRESS_VALUE, 1f)
}