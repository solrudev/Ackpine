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
 * A container of APK [URIs][Uri] to install.
 */
public interface ApkList {

	/**
	 * Returns the size of the underlying list.
	 */
	public val size: Int

	/**
	 * Materializes this [ApkList] to a [List] of [URIs][Uri].
	 */
	public fun toList(): List<Uri>
}

/**
 * A container of APK [URIs][Uri] to install that supports adding elements to it.
 */
public interface MutableApkList : ApkList {

	/**
	 * Adds the specified [apk] to this [MutableApkList].
	 */
	@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
	public fun add(apk: Uri)

	/**
	 * Adds all elements of the given [apks] collection to this [MutableApkList].
	 */
	@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
	public fun addAll(apks: Iterable<Uri>)
}