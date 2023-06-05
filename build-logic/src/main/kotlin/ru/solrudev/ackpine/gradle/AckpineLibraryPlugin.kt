package ru.solrudev.ackpine.gradle

import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.withType
import org.gradle.plugins.signing.SigningExtension
import org.gradle.plugins.signing.SigningPlugin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinAndroidPluginWrapper
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

class AckpineLibraryPlugin : Plugin<Project> {

	override fun apply(target: Project) = target.run {
		val ackpineExtension = extensions.create<AckpineExtension>("ackpine")
		version = rootProject.version
		pluginManager.run {
			apply(LibraryPlugin::class)
			apply(KotlinAndroidPluginWrapper::class)
			apply(MavenPublishPlugin::class)
			apply(SigningPlugin::class)
		}
		configureKotlin()
		configureAndroid()
		if (rootProject.pluginManager.hasPlugin("${Constants.packageName}.publishing")) {
			configurePublishing(
				artifactName = ackpineExtension.moduleName,
				artifactDescription = ackpineExtension.moduleDescription
			)
			configureSigning()
		}
	}

	private fun Project.configureKotlin() {
		extensions.configure<KotlinAndroidProjectExtension>("kotlin") {
			jvmToolchain(17)
			sourceSets.configureEach {
				languageSettings {
					enableLanguageFeature("DataObjects")
				}
			}
		}
		tasks.withType<KotlinJvmCompile>().configureEach {
			compilerOptions {
				jvmTarget.set(JVM_1_8)
				freeCompilerArgs.addAll("-Xexplicit-api=strict", "-Xjvm-default=all")
			}
		}
	}

	private fun Project.configureAndroid() = extensions.configure<LibraryExtension>("android") {
		compileSdk = 33
		buildToolsVersion = "33.0.2"
		namespace = Constants.packageName

		defaultConfig {
			minSdk = 16
			consumerProguardFiles("consumer-rules.pro")
		}

		publishing {
			singleVariant("release") {
				withSourcesJar()
			}
		}

		buildTypes {
			named("release") {
				isMinifyEnabled = false
				proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
			}
		}

		compileOptions {
			sourceCompatibility = JavaVersion.VERSION_1_8
			targetCompatibility = JavaVersion.VERSION_1_8
		}
	}

	private fun Project.configurePublishing(artifactName: String, artifactDescription: String) = afterEvaluate {
		extensions.configure<PublishingExtension>("publishing") {
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

	private fun Project.configureSigning() = extensions.configure<SigningExtension>("signing") {
		val keyId = rootProject.extra[Constants.signingKeyId] as String
		val key = rootProject.extra[Constants.signingKey] as String
		val password = rootProject.extra[Constants.signingPassword] as String
		useInMemoryPgpKeys(keyId, key, password)
		sign(publishing.publications)
	}

	private val Project.publishing: PublishingExtension
		get() = extensions.getByName("publishing") as PublishingExtension
}