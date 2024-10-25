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

description = "A library providing consistent APIs for installing and uninstalling apps on an Android device"

plugins {
	id("ru.solrudev.ackpine.library")
	id("ru.solrudev.ackpine.library-publish")
	alias(libs.plugins.kotlin.ksp)
}

ackpine {
	id = "core"
	artifact {
		name = "Ackpine Core"
	}
}

dependencies {
	ksp(androidx.room.compiler)
	api(androidx.annotation)
	api(androidx.startup)
	api(libs.listenablefuture)
	api(projects.ackpineResources)
	implementation(projects.ackpineRuntime)
	implementation(androidx.concurrent.futures.core)
	implementation(androidx.core.ktx)
	implementation(androidx.room.runtime)
}

ksp {
	arg("room.schemaLocation", File(projectDir, "schemas").path)
}