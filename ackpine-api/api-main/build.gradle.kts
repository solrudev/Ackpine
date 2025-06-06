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

description = "API definitions for Ackpine"

plugins {
	id("ru.solrudev.ackpine.library")
	id("ru.solrudev.ackpine.library-publish")
	id("ru.solrudev.ackpine.dokka")
}

ackpine {
	id = "api"
	artifact {
		name = "Ackpine API"
		inceptionYear = "2025"
	}
}

dependencies {
	api(libs.listenablefuture)
	api(androidx.annotation)
	api(projects.ackpineResources)
	compileOnly(projects.ackpineApi.apiStubs)
}