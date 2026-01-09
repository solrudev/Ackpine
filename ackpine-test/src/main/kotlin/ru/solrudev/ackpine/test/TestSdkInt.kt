/*
 * Copyright (C) 2026 Ilya Fomichev
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

package ru.solrudev.ackpine.test

import android.os.Build

/**
 * A holder of current Android SDK version for tests.
 */
public object TestSdkInt {

	private val defaultSdkInt = Build.VERSION.SDK_INT.let { sdkInt ->
		if (sdkInt > 0) sdkInt else 34
	}

	@Volatile
	private var value: Int = defaultSdkInt

	/**
	 * Returns currently set SDK version.
	 */
	@JvmStatic
	public fun get(): Int = value

	/**
	 * Sets SDK version to [value].
	 */
	@JvmStatic
	public fun set(value: Int) {
		this.value = value
	}

	/**
	 * Resets SDK version to the default value.
	 */
	@JvmStatic
	public fun reset() {
		value = defaultSdkInt
	}
}