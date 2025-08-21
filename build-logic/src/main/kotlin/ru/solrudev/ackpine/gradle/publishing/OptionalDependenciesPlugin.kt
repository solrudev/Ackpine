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

package ru.solrudev.ackpine.gradle.publishing

import groovy.util.Node
import groovy.util.NodeList
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.provider.Provider
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType

public class OptionalDependenciesPlugin : Plugin<Project> {

	override fun apply(target: Project): Unit = target.run {
		val optionalDependencies = getResolvedOptionalDependencies()
		pluginManager.withPlugin("maven-publish") {
			configurePom(optionalDependencies)
		}
	}

	private fun Project.getResolvedOptionalDependencies(): Provider<ResolvedComponentResult> {
		val optional = configurations.dependencyScope("optional") {
			description = "Acts as compileOnly configuration, but also adds artifacts from this configuration " +
					"to published POM as optional dependencies."
		}
		configurations
			.named { it == "compileOnly" }
			.configureEach {
				extendsFrom(optional.get())
			}
		return configurations
			.resolvable("optionalElements") {
				description = "Resolved configuration for optional dependencies"
				extendsFrom(optional.get())
			}
			.get()
			.incoming
			.resolutionResult
			.rootComponent
	}

	private fun Project.configurePom(
		optionalDependencies: Provider<ResolvedComponentResult>
	) = extensions.configure<PublishingExtension> {
		publications.withType<MavenPublication>().configureEach {
			pom.withXml {
				val dependencies = (asNode()
					.get("dependencies") as NodeList)
					.first() as Node
				optionalDependencies
					.get()
					.dependencies
					.forEach { optionalDependency ->
						val (groupId, artifactId, version) = optionalDependency.requested.displayName.split(':')
						dependencies.appendNode("dependency").run {
							appendNode("groupId", groupId)
							appendNode("artifactId", artifactId)
							appendNode("version", version)
							appendNode("optional", true)
						}
					}
			}
		}
	}
}