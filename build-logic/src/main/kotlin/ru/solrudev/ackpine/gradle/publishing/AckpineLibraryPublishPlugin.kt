package ru.solrudev.ackpine.gradle.publishing

import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.getByType
import org.gradle.plugins.signing.SigningExtension
import ru.solrudev.ackpine.gradle.AckpineExtension
import ru.solrudev.ackpine.gradle.Constants

class AckpineLibraryPublishPlugin : Plugin<Project> {

	override fun apply(target: Project) = target.run {
		if (rootProject.pluginManager.hasPlugin("${Constants.packageName}.publishing")) {
			configurePublishing()
			configureSigning()
		}
		if (pluginManager.hasPlugin("${Constants.packageName}.library")) {
			configureSourcesJar()
		}
	}

	private fun Project.configurePublishing() = afterEvaluate {
		val ackpineExtension = extensions.getByType<AckpineExtension>()
		val artifactName = ackpineExtension.moduleName
		val artifactDescription = ackpineExtension.moduleDescription
		extensions.configure<PublishingExtension> {
			publications {
				create<MavenPublication>("release") {
					groupId = rootProject.group.toString()
					artifactId = "ackpine-$artifactName"
					version = rootProject.version.toString()
					from(components.getByName("release"))

					pom {
						name.set(artifactName)
						description.set(artifactDescription)
						url.set("https://github.com/solrudev/Ackpine")

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