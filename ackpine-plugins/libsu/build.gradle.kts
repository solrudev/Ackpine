/*
 * Copyright (C) 2026 Ilya Fomichev
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

description = "Ackpine plugin providing support for installing packages under root user via libsu"

plugins {
	id("ru.solrudev.ackpine.library")
	id("ru.solrudev.ackpine.library-publish")
	id("ru.solrudev.ackpine.dokka")
	alias(libs.plugins.hiddenApiRefine)
	alias(libs.plugins.kotlin.ksp)
	alias(androidx.plugins.room)
}

ackpine {
	id = "libsu"
	minSdk = 21
	artifact {
		name = "Ackpine libsu Plugin"
		inceptionYear = "2026"
	}
	internalPackages("ru.solrudev.ackpine.libsu.database")
}

room {
	schemaDirectory(layout.projectDirectory.dir("schemas"))
}

dependencies {
	ksp(androidx.room.compiler)
	api(projects.ackpinePlugins.privileged)
	implementation(projects.ackpineRuntime)
	implementation(androidx.room.runtime)
	implementation(libs.libsu.service)
	implementation(libs.hiddenApiBypass)
}