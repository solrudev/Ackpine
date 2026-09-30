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

package ru.solrudev.ackpine.gradle.versioning

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.registerIfAbsent

private const val CHANNEL_PROPERTY = "ackpine.version.channel"
private const val LOCAL_CHANNEL = "unplugged.local"

/**
 * Returns a provider of a [Version] object parsed from `version.json` file in root project directory.
 *
 * The version carries the fork's release [channel][Version.channel] from the `ackpine.version.channel` Gradle
 * property. It defaults to `unplugged.local`, so that nothing built outside CI can pass for an upstream release; CI
 * sets it from the branch.
 */
public val Project.ackpineVersion: Provider<Version>
	get() = gradle
		.sharedServices
		.registerIfAbsent("versioning", VersioningService::class) {
			parameters.versionFile = layout.settingsDirectory.file("version.json")
			parameters.channel = providers.gradleProperty(CHANNEL_PROPERTY).orElse(LOCAL_CHANNEL)
		}
		.map { service ->
			service.version
		}