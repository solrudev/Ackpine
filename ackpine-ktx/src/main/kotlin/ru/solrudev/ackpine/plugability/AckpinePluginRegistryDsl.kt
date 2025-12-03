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
public interface AckpinePluginRegistryDsl {

	/**
	 * Applies a [plugin] to a session.
	 * @param plugin Kotlin class of an applied plugin, implementing [AckpinePlugin].
	 * @param parameters parameters of the applied plugin for the session being configured.
	 */
	public fun <Params : Parameters> usePlugin(
		plugin: KClass<out AckpinePlugin<Params>>,
		parameters: Params
	)

	/**
	 * Applies a [plugin] to a session.
	 * @param plugin Kotlin class of an applied plugin, implementing [AckpinePlugin].
	 */
	public fun usePlugin(plugin: KClass<out AckpinePlugin<Parameters.None>>)
}

/**
 * Applies a plugin to a session. [Plugin] is the type of the plugin being applied.
 */
public inline fun <reified Plugin : AckpinePlugin<Parameters.None>> AckpinePluginRegistryDsl.usePlugin() {
	usePlugin(Plugin::class)
}