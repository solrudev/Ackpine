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

import ru.solrudev.ackpine.plugability.AckpinePlugin.Parameters
import kotlin.reflect.KClass

/**
 * DSL allowing to apply [plugins][AckpinePlugin].
 */
@Deprecated(
	message = "Use typed plugin() methods on InstallParametersDsl or UninstallParametersDsl directly. " +
			"This will become an error in the next minor version. " +
			"Using methods of this interface with untyped plugins (implementing AckpinePlugin directly) will throw.",
	level = DeprecationLevel.WARNING
)
public interface AckpinePluginRegistryDsl {

	/**
	 * Applies a [plugin] to a session.
	 * @param plugin Kotlin class of an applied plugin, implementing [AckpinePlugin].
	 * @param parameters parameters of the applied plugin for the session being configured.
	 */
	@Deprecated(
		message = "Use typed plugin() methods on InstallParametersDsl or UninstallParametersDsl directly. " +
				"This will become an error in the next minor version. " +
				"Untyped plugins (implementing AckpinePlugin directly) will throw when used.",
		level = DeprecationLevel.WARNING
	)
	public fun <Params : Parameters> usePlugin(
		plugin: KClass<out AckpinePlugin>,
		parameters: Params
	)

	/**
	 * Applies a [plugin] to a session.
	 * @param plugin Kotlin class of an applied plugin, implementing [AckpinePlugin].
	 */
	@Deprecated(
		message = "Use typed plugin() methods on InstallParametersDsl or UninstallParametersDsl directly. " +
				"This will become an error in the next minor version. " +
				"Untyped plugins (implementing AckpinePlugin directly) will throw when used.",
		level = DeprecationLevel.WARNING
	)
	public fun usePlugin(plugin: KClass<out AckpinePlugin>)
}

/**
 * Applies a plugin to a session. [Plugin] is the type of the plugin being applied.
 */
@Deprecated(
	message = "Use typed plugin() methods on InstallParametersDsl or UninstallParametersDsl directly. " +
			"This will become an error in the next minor version. " +
			"Untyped plugins (implementing AckpinePlugin directly) will throw when used.",
	level = DeprecationLevel.WARNING
)
@Suppress("DEPRECATION")
public inline fun <reified Plugin : AckpinePlugin> AckpinePluginRegistryDsl.usePlugin() {
	usePlugin(Plugin::class)
}