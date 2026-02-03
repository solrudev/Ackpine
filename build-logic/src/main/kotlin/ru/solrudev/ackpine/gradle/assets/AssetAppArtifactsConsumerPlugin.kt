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

package ru.solrudev.ackpine.gradle.assets

import com.android.build.api.variant.LibraryAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvableConfiguration
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.the
import ru.solrudev.ackpine.gradle.helpers.resolvableAppArtifacts
import ru.solrudev.ackpine.gradle.tasks.PrepareAssetAppArtifactsTask

public class AssetAppArtifactsConsumerPlugin : Plugin<Project> {

	override fun apply(target: Project): Unit = target.run {
		pluginManager.withPlugin("com.android.library") {
			val extension = extensions.create(
				"assetAppArtifacts",
				AssetAppArtifactsConsumerExtension::class,
				the<LibraryAndroidComponentsExtension>()
			)
			val appArtifactsAssets = configurations.dependencyScope("assetAppArtifacts")
			val appArtifacts = configurations.resolvableAppArtifacts("appArtifacts") {
				extendsFrom(appArtifactsAssets.get())
			}
			extension.addComponentsSelectionListener { selection ->
				registerPrepareAssetAppArtifactsTasks(selection, appArtifacts)
			}
		}
	}

	private fun Project.registerPrepareAssetAppArtifactsTasks(
		selection: ComponentsSelection,
		appArtifacts: Provider<ResolvableConfiguration>
	) = extensions.configure<LibraryAndroidComponentsExtension> {
		onVariants(selection.selector) { variant ->
			val components = LibraryComponentSelector(variant)
				.apply(selection.action::execute)
				.selectedComponents
			if (components.isEmpty()) {
				return@onVariants
			}
			val taskName = variant.computeTaskName(action = "prepare", subject = "assetAppArtifacts")
			val outputDir = layout.buildDirectory.dir("generated/app_artifacts/${variant.name}")
			val task = tasks.register<PrepareAssetAppArtifactsTask>(taskName) {
				from(appArtifacts)
				into(outputDir)
			}
			for (component in components) {
				component.sources.assets?.addGeneratedSourceDirectory(task) { it.outputDirectory }
			}
		}
	}
}