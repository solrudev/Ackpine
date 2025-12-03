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

package ru.solrudev.ackpine.shizuku

import ru.solrudev.ackpine.session.parameters.SessionParametersDsl

/**
 * DSL allowing to configure [parameters for ShizukuUninstallPlugin][ShizukuUninstallPlugin.Parameters].
 */
@SessionParametersDsl
public interface ShizukuUninstallPluginParametersDsl {

	/**
	 * Flag parameter to indicate that you don't want to delete the package's data directory.
	 */
	public var keepData: Boolean

	/**
	 * Flag parameter to indicate that you want the package deleted for all users.
	 */
	public var allUsers: Boolean
}

@PublishedApi
internal class ShizukuUninstallPluginParametersDslBuilder : ShizukuUninstallPluginParametersDsl {

	private val builder = ShizukuUninstallPlugin.Parameters.Builder()

	override var keepData: Boolean
		get() = builder.keepData
		set(value) {
			builder.setKeepData(value)
		}

	override var allUsers: Boolean
		get() = builder.allUsers
		set(value) {
			builder.setAllUsers(value)
		}

	fun build() = builder.build()
}