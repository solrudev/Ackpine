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

package ru.solrudev.ackpine.helpers

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

@JvmSynthetic
internal fun Uri.displayNameAndSize(context: Context): Pair<String, Long> {
	context.contentResolver.query(
		this,
		arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
		null, null, null
	).use { cursor ->
		cursor ?: return "" to -1L
		if (!cursor.moveToFirst()) {
			return "" to -1L
		}
		return cursor.getString(0).orEmpty() to cursor.getLong(1)
	}
}