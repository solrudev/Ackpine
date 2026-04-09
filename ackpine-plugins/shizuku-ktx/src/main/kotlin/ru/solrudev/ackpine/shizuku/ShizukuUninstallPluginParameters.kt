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

/**
 * Constructs a new instance of [ShizukuPlugin.UninstallParameters].
 */
@Deprecated(
	message = "Use ShizukuUninstallParameters instead. This will become an error in the next minor version.",
	replaceWith = ReplaceWith(
		"ShizukuUninstallParameters(configure)",
		"ru.solrudev.ackpine.shizuku.ShizukuUninstallParameters"
	)
)
@Suppress("FunctionName", "DEPRECATION")
public inline fun ShizukuUninstallPluginParameters(
	configure: ShizukuUninstallPluginParametersDsl.() -> Unit = {}
): ShizukuPlugin.UninstallParameters {
	return ShizukuUninstallPluginParametersDslBuilder().apply(configure).build()
}