/*
 * Copyright (C) 2023 Ilya Fomichev
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

package ru.solrudev.ackpine.gradle.publishing

import com.vanniktech.maven.publish.AndroidMultiVariantLibrary
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.MavenPublishBasePlugin
import com.vanniktech.maven.publish.SonatypeHost
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import ru.solrudev.ackpine.gradle.AckpineArtifact
import ru.solrudev.ackpine.gradle.AckpineLibraryExtension
import ru.solrudev.ackpine.gradle.AckpineLibraryPlugin

public class AckpineLibraryPublishPlugin : Plugin<Project> {

	override fun apply(target: Project): Unit = target.run {
		if (!pluginManager.hasPlugin(AckpineLibraryPlugin.PLUGIN_ID)) {
			error("Applying library-publish plugin requires the library plugin to be applied")
		}
		pluginManager.apply(MavenPublishBasePlugin::class)
		val ackpineLibraryExtension = extensions.getByType<AckpineLibraryExtension>().apply {
			addIdListener { id ->
				configureArtifactCoordinates(id)
			}
		}
		val artifact = ackpineLibraryExtension.extensions.create<AckpineArtifact>("artifact")
		configurePublishing(artifact.name, provider { description })
	}

	private fun Project.configureArtifactCoordinates(id: String) = extensions.configure<MavenPublishBaseExtension> {
		coordinates(group.toString(), artifactId = "ackpine-$id", version.toString())
	}

	private fun Project.configurePublishing(
		artifactName: Provider<String>,
		artifactDescription: Provider<String>
	) = extensions.configure<MavenPublishBaseExtension> {
		configure(
			AndroidMultiVariantLibrary(
				includedBuildTypeValues = setOf("release"),
				sourcesJar = true,
				publishJavadocJar = false
			)
		)
		publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
		signAllPublications()

		pom {
			name = artifactName
			description = artifactDescription
			inceptionYear = "2023"
			url = "https://ackpine.solrudev.ru"

			licenses {
				license {
					name = "The Apache Software License, Version 2.0"
					url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
				}
			}

			developers {
				developer {
					id = "solrudev"
					name = "Ilya Fomichev"
				}
			}

			scm {
				connection = "scm:git:github.com/solrudev/Ackpine.git"
				developerConnection = "scm:git:ssh://github.com/solrudev/Ackpine.git"
				url = "https://github.com/solrudev/Ackpine/tree/master"
			}
		}
	}
}