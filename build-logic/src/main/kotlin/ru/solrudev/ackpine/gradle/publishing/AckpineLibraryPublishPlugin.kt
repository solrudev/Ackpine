/*
 * Copyright (C) 2023-2024 Ilya Fomichev
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

import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.MavenPublishPlugin
import com.vanniktech.maven.publish.SonatypeHost
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.hasPlugin
import org.gradle.kotlin.dsl.withType
import org.jetbrains.dokka.gradle.DokkaPlugin
import org.jetbrains.dokka.gradle.DokkaTaskPartial
import ru.solrudev.ackpine.gradle.AckpineArtifact
import ru.solrudev.ackpine.gradle.AckpineExtension
import ru.solrudev.ackpine.gradle.AckpineLibraryPlugin

public class AckpineLibraryPublishPlugin : Plugin<Project> {

	override fun apply(target: Project): Unit = target.run {
		check(rootProject.plugins.hasPlugin(AckpinePublishingPlugin::class)) {
			"Applying library-publish plugin requires the publishing plugin to be applied to the root project"
		}
		check(plugins.hasPlugin(AckpineLibraryPlugin::class)) {
			"Applying library-publish plugin requires the library plugin to be applied"
		}
		pluginManager.run {
			apply(MavenPublishPlugin::class)
			apply(DokkaPlugin::class)
		}
		val ackpineExtension = extensions.getByType<AckpineExtension>()
		val artifact = ackpineExtension.extensions.create<AckpineArtifact>("artifact")
		configureDokka(artifact)
		configurePublishing(ackpineExtension, artifact)
	}

	private fun Project.configureDokka(artifact: AckpineArtifact) = afterEvaluate {
		tasks.withType<DokkaTaskPartial>().configureEach {
			enabled = artifact.dokka.get()
		}
	}

	private fun Project.configurePublishing(
		ackpineExtension: AckpineExtension,
		artifact: AckpineArtifact
	) = extensions.configure<MavenPublishBaseExtension> {
		configure(
			AndroidSingleVariantLibrary(
				variant = "release",
				sourcesJar = true,
				publishJavadocJar = false
			)
		)
		publishToMavenCentral(SonatypeHost.S01)
		signAllPublications()

		afterEvaluate {
			coordinates(group.toString(), artifactId = "ackpine-${ackpineExtension.id}", version.toString())

			pom {
				name = artifact.name
				description = this@afterEvaluate.description
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
}