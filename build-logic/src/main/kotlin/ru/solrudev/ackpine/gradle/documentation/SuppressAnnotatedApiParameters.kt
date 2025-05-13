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

package ru.solrudev.ackpine.gradle.documentation

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.addAll
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.putJsonArray
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.jetbrains.dokka.gradle.engine.plugins.DokkaPluginParametersBaseSpec
import org.jetbrains.dokka.gradle.internal.InternalDokkaGradlePluginApi
import javax.inject.Inject

@OptIn(InternalDokkaGradlePluginApi::class)
public abstract class SuppressAnnotatedApiParameters @Inject constructor(
	name: String
) : DokkaPluginParametersBaseSpec(
	name,
	pluginFqn = "ru.solrudev.ackpine.documentation.SuppressAnnotatedApiDokkaPlugin"
) {

	@get:Input
	public abstract val annotatedWith: SetProperty<String>

	@OptIn(ExperimentalSerializationApi::class)
	override fun jsonEncode(): String = buildJsonObject {
		putJsonArray("annotatedWith") {
			addAll(annotatedWith.get())
		}
	}.toString()

	private companion object {
		private const val serialVersionUID = 1281126468724316674L
	}
}