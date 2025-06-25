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

package ru.solrudev.ackpine.documentation

import kotlinx.serialization.json.Json
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.transformers.documentables.SuppressedByConditionDocumentableFilterTransformer
import org.jetbrains.dokka.model.Annotations
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.DokkaPluginApiPreview
import org.jetbrains.dokka.plugability.Extension
import org.jetbrains.dokka.plugability.PluginApiPreviewAcknowledgement
import org.jetbrains.dokka.transformers.documentation.PreMergeDocumentableTransformer

/**
 * Dokka plugin allowing to filter APIs annotated with configured annotations from resulting documentation artifacts.
 */
public class SuppressAnnotatedApiDokkaPlugin : DokkaPlugin() {

	public val filterExtension: Extension<PreMergeDocumentableTransformer, *, *> by extending {
		plugin<DokkaBase>().preMergeDocumentableTransformer providing ::SuppressAnnotatedApiTransformer
	}

	@OptIn(DokkaPluginApiPreview::class)
	override fun pluginApiPreviewAcknowledgement(): PluginApiPreviewAcknowledgement = PluginApiPreviewAcknowledgement
}

private class SuppressAnnotatedApiTransformer(
	context: DokkaContext
) : SuppressedByConditionDocumentableFilterTransformer(context) {

	private val configuration by lazy {
		val config = context
			.configuration
			.pluginsConfiguration
			.firstOrNull { it.fqPluginName == "ru.solrudev.ackpine.documentation.SuppressAnnotatedApiDokkaPlugin" }
		requireNotNull(config) { "SuppressAnnotatedApiDokkaPlugin configuration was not found" }
		Json.decodeFromString(SuppressAnnotatedApiConfig.serializer(), config.values)
	}

	override fun shouldBeSuppressed(d: Documentable): Boolean {
		val documentableWithExtras = d as? WithExtraProperties<*> ?: return false
		return documentableWithExtras
			.extra
			.allOfType<Annotations>()
			.flatMap { it.directAnnotations.values.flatten() }
			.any { annotation ->
				val annotationFqn = "${annotation.dri.packageName}.${annotation.dri.classNames}"
				annotationFqn in configuration.annotatedWith
			}
	}
}