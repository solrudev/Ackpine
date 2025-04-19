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

package ru.solrudev.ackpine.impl.helpers.concurrent

import android.annotation.SuppressLint
import java.util.concurrent.ConcurrentMap

// To avoid referencing Build.VERSION.SDK_INT
private val isComputeIfAbsentAvailable = try {
	ConcurrentMap::class.java.getMethod(
		"computeIfAbsent",
		Any::class.java,
		Class.forName("java.util.function.Function")
	)
	true
} catch (_: NoSuchMethodException) {
	false
} catch (_: ClassNotFoundException) {
	false
}

/**
 * Calls [computeIfAbsent][ConcurrentMap.computeIfAbsent] if available, otherwise falls back to [getOrPut].
 */
@SuppressLint("NewApi")
@JvmSynthetic
internal fun <K, V> ConcurrentMap<K, V>.computeIfAbsentCompat(key: K, defaultValue: () -> V): V {
	if (isComputeIfAbsentAvailable) {
		return computeIfAbsent(key) { defaultValue() }
	}
	return getOrPut(key, defaultValue)
}