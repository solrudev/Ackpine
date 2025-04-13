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

package ru.solrudev.ackpine.impl.helpers

import android.content.Intent
import android.os.Build
import android.os.Parcelable

@Suppress("DEPRECATION")
@JvmSynthetic
internal inline fun <reified T : Parcelable> Intent.getParcelableExtraCompat(name: String): T? {
	// https://issuetracker.google.com/issues/240585930#comment6
	return if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU) {
		getParcelableExtra(name, T::class.java)
	} else {
		getParcelableExtra(name)
	}
}