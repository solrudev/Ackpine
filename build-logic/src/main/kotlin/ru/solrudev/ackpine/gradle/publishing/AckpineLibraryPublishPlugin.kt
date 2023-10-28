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

import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.hasPlugin
import org.gradle.plugins.signing.SigningExtension
import org.gradle.plugins.signing.SigningPlugin
import org.jetbrains.dokka.gradle.DokkaPlugin
import ru.solrudev.ackpine.gradle.AckpineArtifact
import ru.solrudev.ackpine.gradle.AckpineExtension
import ru.solrudev.ackpine.gradle.AckpineLibraryPlugin
import ru.solrudev.ackpine.gradle.Constants

public class AckpineLibraryPublishPlugin : Plugin<Project> {

	override fun apply(target: Project): Unit = target.run {
		if (rootProject.plugins.hasPlugin(AckpinePublishingPlugin::class)) {
			pluginManager.run {
				apply(MavenPublishPlugin::class)
				apply(SigningPlugin::class)
				apply(DokkaPlugin::class)
			}
			val ackpineExtension = extensions.getByType<AckpineExtension>()
			val artifact = ackpineExtension.extensions.create<AckpineArtifact>("artifact")
			configurePublishing(ackpineExtension, artifact)
			configureSigning()
		}
		if (plugins.hasPlugin(AckpineLibraryPlugin::class)) {
			configureSourcesJar()
		}
	}

	private fun Project.configurePublishing(
		ackpineExtension: AckpineExtension,
		artifact: AckpineArtifact
	) = afterEvaluate {
		extensions.configure<PublishingExtension> {
			publications {
				create<MavenPublication>("release") {
					groupId = this@afterEvaluate.group.toString()
					artifactId = "ackpine-${ackpineExtension.id}"
					version = this@afterEvaluate.version.toString()
					from(components.getByName("release"))

					pom {
						name.set(artifact.name)
						description.set(this@afterEvaluate.description)
						url.set("https://solrudev.github.io/Ackpine")

						licenses {
							license {
								name.set("The Apache Software License, Version 2.0")
								url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
							}
						}

						developers {
							developer {
								id.set("solrudev")
								name.set("Ilya Fomichev")
							}
						}

						scm {
							connection.set("scm:git:github.com/solrudev/Ackpine.git")
							developerConnection.set("scm:git:ssh://github.com/solrudev/Ackpine.git")
							url.set("https://github.com/solrudev/Ackpine/tree/master")
						}
					}
				}
			}
		}
	}

	private fun Project.configureSigning() = extensions.configure<SigningExtension> {
		val keyId = rootProject.extra[Constants.signingKeyId] as String
		val key = rootProject.extra[Constants.signingKey] as String
		val password = rootProject.extra[Constants.signingPassword] as String
		useInMemoryPgpKeys(keyId, key, password)
		sign(extensions.getByType<PublishingExtension>().publications)
	}

	private fun Project.configureSourcesJar() = extensions.configure<LibraryExtension> {
		publishing {
			singleVariant("release") {
				withSourcesJar()
			}
		}
	}
}