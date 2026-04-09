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

package ru.solrudev.ackpine.plugability

import ru.solrudev.ackpine.installer.parameters.InstallParameters
import ru.solrudev.ackpine.uninstaller.parameters.UninstallParameters

/**
 * A plugin for Ackpine. Allows to extend Ackpine's functionality.
 *
 * New plugins should implement [AckpineInstallPlugin] and/or [AckpineUninstallPlugin] instead of overriding the
 * [apply] methods on this interface directly.
 */
public interface AckpinePlugin {

	/**
	 * Unique ID of the plugin.
	 */
	public val id: String

	/**
	 * Applies some settings to install parameters to accommodate the plugin's functionality.
	 */
	@Deprecated(
		message = "Implement AckpineInstallPlugin instead. This will become an error in the next minor version.",
		level = DeprecationLevel.WARNING
	)
	public fun apply(builder: InstallParameters.Builder) { // no-op by default
	}

	/**
	 * Applies some settings to uninstall parameters to accommodate the plugin's functionality.
	 */
	@Deprecated(
		message = "Implement AckpineUninstallPlugin instead. This will become an error in the next minor version.",
		level = DeprecationLevel.WARNING
	)
	public fun apply(builder: UninstallParameters.Builder) { // no-op by default
	}

	/**
	 * A set of parameters for an Ackpine plugin.
	 */
	public interface Parameters {

		/**
		 * Empty plugin parameters for plugins without configuration.
		 */
		public data object None : Parameters
	}
}