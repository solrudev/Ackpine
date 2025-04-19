/*
 * Copyright (C) 2023-2025 Ilya Fomichev
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

package ru.solrudev.ackpine.impl.helpers

import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import java.io.Serializable

@Suppress("DEPRECATION")
@JvmSynthetic
internal inline fun <reified T : Parcelable> Bundle.getParcelableCompat(name: String): T? {
	// https://issuetracker.google.com/issues/240585930#comment6
	return if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU) {
		getParcelable(name, T::class.java)
	} else {
		getParcelable(name)
	}
}

@Suppress("DEPRECATION")
@JvmSynthetic
internal inline fun <reified T : Serializable> Bundle.getSerializableCompat(name: String): T? {
	// https://issuetracker.google.com/issues/240585930#comment6
	return if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU) {
		getSerializable(name, T::class.java)
	} else {
		getSerializable(name) as? T
	}
}