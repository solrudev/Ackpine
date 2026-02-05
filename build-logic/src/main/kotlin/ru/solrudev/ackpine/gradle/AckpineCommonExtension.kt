/*
 * Copyright (C) 2024 Ilya Fomichev
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

package ru.solrudev.ackpine.gradle

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Action
import ru.solrudev.ackpine.gradle.testing.AckpineTestingOptions

internal typealias IdListener = (id: String) -> Unit

/**
 * Common extension properties for Ackpine `library` and `sample` plugins.
 */
public abstract class AckpineCommonExtension(
	private val commonExtension: CommonExtension,
	private val packageName: String,

	/**
	 * Configures testing for Ackpine module.
	 */
	public val testing: AckpineTestingOptions
) {

	private val idListeners = mutableSetOf<IdListener>()
	private var _id = packageName

	/**
	 * Ackpine library ID used in namespace of the generated R and BuildConfig classes, in artifact ID
	 * and in application ID.
	 */
	public open var id: String
		get() = _id
		set(value) {
			_id = value
			commonExtension.namespace = "$packageName.${value.replace('-', '.')}"
			for (listener in idListeners) {
				listener(value)
			}
		}

	/**
	 * Minimum SDK version.
	 */
	public var minSdk: Int?
		get() = commonExtension.defaultConfig.minSdk
		set(value) {
			commonExtension.defaultConfig.minSdk = value
		}

	/**
	 * Configures testing for Ackpine module.
	 */
	public fun testing(action: Action<AckpineTestingOptions>) {
		action.execute(testing)
	}

	/**
	 * Adds a [listener] which will be called when [id] is set.
	 */
	internal fun addIdListener(listener: IdListener) {
		idListeners += listener
	}
}