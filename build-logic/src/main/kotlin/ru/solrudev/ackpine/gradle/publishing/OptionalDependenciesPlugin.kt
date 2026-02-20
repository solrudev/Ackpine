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
import org.gradle.api.artifacts.ExternalDependency
import org.gradle.api.provider.Provider
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType

public class OptionalDependenciesPlugin : Plugin<Project> {

	override fun apply(target: Project): Unit = target.run {
		val optionalDependencies = getOptionalDependencies()
		pluginManager.withPlugin("maven-publish") {
			configurePom(optionalDependencies)
		}
	}

	private fun Project.getOptionalDependencies(): Provider<List<DependencyDescriptor>> {
		val optional = configurations.dependencyScope("optional") {
			description = "Acts as compileOnly configuration, but also adds artifacts from this configuration " +
					"to published POM as optional dependencies."
			dependencies.configureEach {
				check(this is ExternalDependency) {
					"Optional dependency ($this) is not an external module dependency. " +
							"Only external module dependencies are supported."
				}
			}
		}
		configurations
			.named { it == "compileOnly" }
			.configureEach {
				extendsFrom(optional.get())
			}
		return optional.map { optionalConfiguration ->
			optionalConfiguration.dependencies.map { dependency ->
				DependencyDescriptor(
					checkNotNull(dependency.group),
					dependency.name,
					checkNotNull(dependency.version)
				)
			}
		}
	}

	private fun Project.configurePom(
		optionalDependenciesProvider: Provider<List<DependencyDescriptor>>
	) = extensions.configure<PublishingExtension> {
		publications.withType<MavenPublication>().configureEach {
			pom.withXml {
				val optionalDependencies = optionalDependenciesProvider.get()
				if (optionalDependencies.isEmpty()) {
					return@withXml
				}
				val root = asNode()
				val dependencies = (root.get("dependencies") as NodeList).firstOrNull() as Node?
					?: root.appendNode("dependencies")
				for (optionalDependency in optionalDependencies) {
					dependencies.appendNode("dependency").run {
						appendNode("groupId", optionalDependency.group)
						appendNode("artifactId", optionalDependency.name)
						appendNode("version", optionalDependency.version)
						appendNode("optional", true)
					}
				}
			}
		}
	}

	private class DependencyDescriptor(val group: String, val name: String, val version: String)
}