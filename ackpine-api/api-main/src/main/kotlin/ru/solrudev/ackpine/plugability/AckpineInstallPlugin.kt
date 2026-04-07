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

package ru.solrudev.ackpine.plugability

import ru.solrudev.ackpine.installer.parameters.InstallParameters

/**
 * A plugin for Ackpine which extends install session functionality.
 */
public interface AckpineInstallPlugin<Params : AckpinePlugin.Parameters> : AckpinePlugin {

	/**
	 * Applies some settings to install session scope to accommodate the plugin's functionality.
	 */
	public fun apply(scope: InstallPluginScope) { // no-op by default
	}

	@Deprecated(
		"Implement AckpineInstallPlugin instead. This will become an error in the next minor version. " +
				"If overridden, this overload will be prioritized over AckpineInstallPlugin.apply(scope).",
		level = DeprecationLevel.WARNING
	)
	override fun apply(builder: InstallParameters.Builder) {
		apply(builder.pluginScope)
	}
}