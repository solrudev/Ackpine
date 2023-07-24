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

package ru.solrudev.ackpine.installer.parameters

import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Constructs a new instance of [InstallParameters].
 */
public inline fun InstallParameters(
	baseApk: Uri,
	configure: InstallParametersDsl.() -> Unit
): InstallParameters {
	return InstallParametersDslBuilder(baseApk).apply(configure).build()
}

/**
 * Constructs a new instance of [InstallParameters].
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public inline fun InstallParameters(
	apks: Iterable<Uri>,
	configure: InstallParametersDsl.() -> Unit
): InstallParameters {
	return InstallParametersDslBuilder(apks).apply(configure).build()
}