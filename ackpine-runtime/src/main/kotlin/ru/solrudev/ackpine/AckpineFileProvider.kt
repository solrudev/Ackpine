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

package ru.solrudev.ackpine

import android.content.Context
import android.content.pm.ProviderInfo
import androidx.annotation.RestrictTo
import androidx.core.content.FileProvider
import ru.solrudev.ackpine.runtime.R

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AckpineFileProvider : FileProvider(R.xml.ackpine_file_provider_paths) {

	override fun attachInfo(context: Context, info: ProviderInfo) {
		super.attachInfo(context, info)
		authority = info.authority
	}

	public companion object {
		public lateinit var authority: String
			private set
	}
}