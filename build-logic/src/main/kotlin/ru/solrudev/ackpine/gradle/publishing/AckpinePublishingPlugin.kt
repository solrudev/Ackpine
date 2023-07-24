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

import io.github.gradlenexus.publishplugin.NexusPublishExtension
import io.github.gradlenexus.publishplugin.NexusPublishPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.extra
import org.jetbrains.dokka.gradle.DokkaPlugin
import ru.solrudev.ackpine.gradle.Constants
import ru.solrudev.ackpine.gradle.helpers.withProperties

class AckpinePublishingPlugin : Plugin<Project> {

	override fun apply(target: Project) = target.run {
		require(this == rootProject) { "Plugin must be applied to the root project but was applied to $path" }
		group = Constants.packageName
		version = versionFromPropertiesFile()
		pluginManager.run {
			apply(NexusPublishPlugin::class)
			apply(DokkaPlugin::class)
		}
		val publishingProperties = publishingFromPropertiesFile()
		val ossrhUsername = System.getenv("OSSRH_USERNAME")
			?: publishingProperties["ossrhUsername"]
		val ossrhPassword = System.getenv("OSSRH_PASSWORD")
			?: publishingProperties["ossrhPassword"]
		val sonatypeStagingProfileId = System.getenv("SONATYPE_STAGING_PROFILE_ID")
			?: publishingProperties["sonatypeStagingProfileId"]
		extra[Constants.signingKeyId] = System.getenv("SIGNING_KEY_ID")
			?: publishingProperties["signing.keyId"].orEmpty()
		extra[Constants.signingKey] = System.getenv("SIGNING_KEY")
			?: publishingProperties["signing.key"].orEmpty()
		extra[Constants.signingPassword] = System.getenv("SIGNING_PASSWORD")
			?: publishingProperties["signing.password"].orEmpty()
		extensions.configure<NexusPublishExtension> {
			repositories {
				sonatype {
					stagingProfileId.set(sonatypeStagingProfileId)
					username.set(ossrhUsername)
					password.set(ossrhPassword)
					nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
					snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
				}
			}
		}
	}

	private fun Project.versionFromPropertiesFile(): String = file("version.properties").withProperties {
		val majorVersion = get("MAJOR_VERSION") as String
		val minorVersion = get("MINOR_VERSION") as String
		val patchVersion = get("PATCH_VERSION") as String
		val suffix = get("SUFFIX") as String
		val isSnapshot = (get("SNAPSHOT") as String).toBooleanStrict()
		return buildString {
			append("$majorVersion.$minorVersion.$patchVersion")
			if (suffix.isNotEmpty()) {
				append("-$suffix")
			}
			if (isSnapshot) {
				append("-SNAPSHOT")
			}
		}
	}

	private fun Project.publishingFromPropertiesFile() = buildMap map@{
		val secretPropertiesFile = file("local.properties")
		if (secretPropertiesFile.exists()) {
			secretPropertiesFile.withProperties {
				forEach { name, value -> this@map.put(name as String, value as String) }
			}
		}
	}
}