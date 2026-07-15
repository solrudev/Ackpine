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

package ru.solrudev.ackpine.privileged

import ru.solrudev.ackpine.session.parameters.SessionParametersDsl

/**
 * DSL allowing to configure [uninstall parameters for privileged Ackpine plugins][PrivilegedUninstallParameters].
 */
@SessionParametersDsl
public interface PrivilegedUninstallParametersDsl {

	/**
	 * Flag parameter to indicate that you don't want to delete the package's data directory.
	 */
	public var keepData: Boolean

	/**
	 * Flag parameter to indicate that you want the package deleted for all users.
	 */
	public var allUsers: Boolean

	/**
	 * Flag parameter to indicate that a system app should be marked as uninstalled for current user.
	 *
	 * This does not remove the app from the system partition. For an updated system app, it prevents the update from
	 * being rolled back globally when uninstalling it for current user.
	 */
	public var systemApp: Boolean
}

/**
 * Base implementation of [PrivilegedUninstallParametersDsl] backed by [PrivilegedUninstallParameters.Builder].
 */
public abstract class PrivilegedUninstallParametersDslBuilder<
		Params : PrivilegedUninstallParameters,
		Builder : PrivilegedUninstallParameters.Builder<Params, Builder>
		> protected constructor(
	private val delegate: Builder
) : PrivilegedUninstallParametersDsl {

	override var keepData: Boolean
		get() = delegate.keepData
		set(value) {
			delegate.setKeepData(value)
		}

	override var allUsers: Boolean
		get() = delegate.allUsers
		set(value) {
			delegate.setAllUsers(value)
		}

	override var systemApp: Boolean
		get() = delegate.systemApp
		set(value) {
			delegate.setSystemApp(value)
		}

	/**
	 * Builds and returns a [Params] instance from the current DSL state.
	 */
	protected fun buildParameters(): Params = delegate.build()
}