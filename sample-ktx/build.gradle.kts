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

description = "Sample application in Kotlin showcasing Ackpine usage and leveraging ackpine-ktx extensions"

plugins {
	id("ru.solrudev.ackpine.sample.kotlin")
	id("ru.solrudev.ackpine.app-release")
	id("kotlin-parcelize")
}

dependencies {
	implementation(projects.ackpineCore)
	implementation(projects.ackpineKtx)
	implementation(projects.ackpineSplits.splitsKtx)
	implementation(projects.ackpineResources)
	implementation(androidx.activity)
	implementation(androidx.appCompat)
	implementation(androidx.documentFile)
	implementation(androidx.recyclerView)
	implementation(androidx.constraintLayout)
	implementation(androidx.coordinatorLayout)
	implementation(androidx.lifecycle.viewModel)
	implementation(androidx.bundles.navigation)
	implementation(androidx.swipeRefreshLayout)
	implementation(libs.materialComponents)
	implementation(libs.vbpd)
	implementation(libs.insetter)
}