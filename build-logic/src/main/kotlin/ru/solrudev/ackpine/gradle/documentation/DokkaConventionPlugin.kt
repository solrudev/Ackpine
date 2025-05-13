/*
 * Copyright (C) 2024 Ilya Fomichev
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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.registerBinding
import org.gradle.kotlin.dsl.the
import org.jetbrains.dokka.gradle.DokkaExtension
import org.jetbrains.dokka.gradle.DokkaPlugin
import org.jetbrains.dokka.gradle.engine.plugins.DokkaHtmlPluginParameters
import ru.solrudev.ackpine.gradle.AckpineLibraryExtension
import ru.solrudev.ackpine.gradle.AckpineLibraryPlugin
import ru.solrudev.ackpine.gradle.versioning.versionNumber
import java.net.URI

public class DokkaConventionPlugin : Plugin<Project> {

	override fun apply(target: Project): Unit = target.run {
		pluginManager.apply(DokkaPlugin::class)
		dependencies.add("dokkaPlugin", dokkaPlugin("suppress-annotated-api"))
		configureDokka()
		pluginManager.withPlugin(AckpineLibraryPlugin.PLUGIN_ID) {
			val internalPackages = the<AckpineLibraryExtension>().internalPackages
			configureDokkaSuppressedFiles(internalPackages)
		}
	}

	private fun Project.configureDokka() = extensions.configure<DokkaExtension> {
		moduleVersion = versionNumber.get().toString()
		pluginsConfiguration.registerBinding(
			SuppressAnnotatedApiParameters::class,
			SuppressAnnotatedApiParameters::class
		)
		pluginsConfiguration.register<SuppressAnnotatedApiParameters>("suppressAnnotatedApi") {
			annotatedWith.add("androidx.annotation.RestrictTo")
		}
		pluginsConfiguration.named<DokkaHtmlPluginParameters>("html") {
			customAssets.from(isolated.rootProject.projectDirectory.file("docs/images/logo-icon.svg"))
			customStyleSheets.from(isolated.rootProject.projectDirectory.file("docs/css/logo-styles.css"))
			footerMessage = "© Ilya Fomichev"
		}
		dokkaSourceSets.configureEach {
			externalDocumentationLinks.register("kotlinx.coroutines") {
				url = URI("https://kotlinlang.org/api/kotlinx.coroutines/")
			}
			externalDocumentationLinks.register("guava") {
				url = URI("https://guava.dev/releases/snapshot/api/docs/")
				packageListUrl = URI("https://guava.dev/releases/snapshot/api/docs/element-list")
			}
		}
	}

	private fun Project.configureDokkaSuppressedFiles(
		internalPackages: Provider<Set<String>>
	) = extensions.configure<DokkaExtension> {
		dokkaSourceSets.configureEach {
			val suppressedPackages = internalPackages.get()
			if (suppressedPackages.isNotEmpty()) {
				val internalSources = sourceRoots.asFileTree.matching {
					for (packageName in suppressedPackages) {
						val packagePath = packageName.replace('.', '/')
						include("**/$packagePath/**")
					}
				}
				suppressedFiles.from(internalSources)
			}
		}
	}
}