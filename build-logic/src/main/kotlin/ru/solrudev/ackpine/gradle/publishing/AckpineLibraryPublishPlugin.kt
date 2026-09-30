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
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.PasswordCredentials
import org.gradle.api.provider.Provider
import org.gradle.api.publish.PublishingExtension
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.the
import ru.solrudev.ackpine.gradle.AckpineLibraryBasePlugin
import ru.solrudev.ackpine.gradle.AckpineLibraryExtension

private const val UNPLUGGED_REPOSITORY_URL = "https://unplugged.jfrog.io/artifactory/unplugged-libraries"

public class AckpineLibraryPublishPlugin : Plugin<Project> {

	override fun apply(target: Project): Unit = target.run {
		pluginManager.withPlugin(AckpineLibraryBasePlugin.PLUGIN_ID) {
			pluginManager.apply(MavenPublishBasePlugin::class)
			val ackpineLibraryExtension = the<AckpineLibraryExtension>().apply {
				addIdListener { id ->
					configureArtifactCoordinates(id)
				}
			}
			val artifact = ackpineLibraryExtension.extensions.create<AckpineArtifact>("artifact")
			artifact.name.convention("")
			artifact.inceptionYear.convention("2023")
			configurePublishing(artifact.name, provider { description }, artifact.inceptionYear)
			configureUnpluggedRepository()
		}
	}

	/**
	 * Adds the werunplugged Artifactory, which the UP Store resolves Ackpine from, as the `unplugged` repository:
	 * `publishAllPublicationsToUnpluggedRepository` publishes every library there with its POM and Gradle module
	 * metadata. Credentials come from the `unpluggedUsername` and `unpluggedPassword` Gradle properties (e.g.
	 * `ORG_GRADLE_PROJECT_unpluggedUsername`) and are only needed when publishing; `ackpine.publishing.unplugged.url`
	 * overrides the URL.
	 */
	private fun Project.configureUnpluggedRepository() = extensions.configure<PublishingExtension> {
		repositories.maven {
			name = "unplugged"
			url = uri(providers.gradleProperty("ackpine.publishing.unplugged.url").getOrElse(UNPLUGGED_REPOSITORY_URL))
			credentials(PasswordCredentials::class.java)
		}
	}

	private fun Project.configureArtifactCoordinates(id: String) = extensions.configure<MavenPublishBaseExtension> {
		coordinates(group.toString(), artifactId = "ackpine-$id", version.toString())
	}

	private fun Project.configurePublishing(
		artifactName: Provider<String>,
		artifactDescription: Provider<String>,
		artifactInceptionYear: Provider<String>
	) = extensions.configure<MavenPublishBaseExtension> {
		configure(
			AndroidMultiVariantLibrary(
				includedBuildTypeValues = setOf("release")
			)
		)
		publishToMavenCentral()
		// Maven Central requires signed publications, the company Artifactory has no PGP key: CI passes
		// -Packpine.publishing.sign=false when publishing there.
		if (providers.gradleProperty("ackpine.publishing.sign").orNull != "false") {
			signAllPublications()
		}

		pom {
			name = artifactName
			description = artifactDescription
			inceptionYear = artifactInceptionYear
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