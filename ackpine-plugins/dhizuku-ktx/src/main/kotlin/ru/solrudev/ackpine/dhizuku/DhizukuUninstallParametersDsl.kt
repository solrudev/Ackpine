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

package ru.solrudev.ackpine.dhizuku

import ru.solrudev.ackpine.DelicateAckpineApi
import ru.solrudev.ackpine.session.parameters.SessionParametersDsl

/**
 * DSL allowing to configure [uninstall parameters for DhizukuPlugin][DhizukuPlugin.UninstallParameters].
 */
@SessionParametersDsl
public interface DhizukuUninstallParametersDsl {

	/**
	 * Whether to retain the package's data directory.
	 */
	public var keepData: Boolean

	/**
	 * Whether to delete the package for all users.
	 */
	public var allUsers: Boolean

	/**
	 * Whether a system app should be marked as uninstalled for the current user.
	 */
	public var systemApp: Boolean

	/**
	 * Raw Android uninstall flags mask, `0` by default. All `Int` bit patterns are accepted.
	 *
	 * The mask is applied _before_ Dhizuku's boolean options. These options take precedence for their corresponding
	 * bits, including clearing them when `false`. Flags without corresponding Dhizuku boolean options remain unchanged.
	 * The boolean values remain independent of this mask.
	 * Setting the mask replaces the previous supplied mask.
	 *
	 * Setting this value is a **delicate** API: flags availability and behavior may depend on the Android version
	 * and device-owner permissions. Supplying flags grants no additional privileges.
	 */
	@set:DelicateAckpineApi
	public var uninstallFlags: Int
}

@PublishedApi
internal class DhizukuUninstallParametersDslBuilder : DhizukuUninstallParametersDsl {

	private val delegate = DhizukuPlugin.UninstallParameters.Builder()

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

	@set:DelicateAckpineApi
	override var uninstallFlags: Int
		get() = delegate.uninstallFlags
		set(value) {
			delegate.setUninstallFlags(value)
		}

	fun build(): DhizukuPlugin.UninstallParameters = delegate.build()
}