/*
 * Copyright (C) 2023-2024 Ilya Fomichev
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

package ru.solrudev.ackpine.splits.parsing

import androidx.annotation.RestrictTo

private const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class AndroidManifest internal constructor(private val manifest: Map<String, String>) {

	@get:JvmSynthetic
	internal val splitName: String
		get() = manifest["split"].orEmpty()

	@get:JvmSynthetic
	internal val packageName: String
		get() = manifest.getValue("package")

	@get:JvmSynthetic
	internal val versionCode: Long
		get() = manifest.getValue("$ANDROID_NAMESPACE:versionCode").toLong()

	@get:JvmSynthetic
	internal val versionName: String
		get() = manifest["$ANDROID_NAMESPACE:versionName"].orEmpty()

	@get:JvmSynthetic
	internal val isFeatureSplit: Boolean
		get() = manifest["$ANDROID_NAMESPACE:isFeatureSplit"]?.toBooleanStrict() == true

	@get:JvmSynthetic
	internal val configForSplit: String
		get() = manifest["configForSplit"].orEmpty()
}