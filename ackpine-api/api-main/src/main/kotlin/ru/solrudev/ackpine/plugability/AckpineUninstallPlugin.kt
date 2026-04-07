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

import ru.solrudev.ackpine.uninstaller.parameters.UninstallParameters

/**
 * A plugin for Ackpine which extends uninstall session functionality.
 */
public interface AckpineUninstallPlugin<Params : AckpinePlugin.Parameters> : AckpinePlugin {

	/**
	 * Applies some settings to uninstall session scope to accommodate the plugin's functionality.
	 */
	public fun apply(scope: UninstallPluginScope) { // no-op by default
	}

	@Deprecated(
		"Implement AckpineUninstallPlugin instead. This will become an error in the next minor version.",
		level = DeprecationLevel.WARNING
	)
	override fun apply(builder: UninstallParameters.Builder) {
		apply(builder.pluginScope)
	}
}