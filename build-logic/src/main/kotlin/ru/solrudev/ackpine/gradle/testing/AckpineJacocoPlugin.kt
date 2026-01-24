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

package ru.solrudev.ackpine.gradle.testing

import com.android.build.api.artifact.ScopedArtifact
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.variant.DeviceTestBuilder
import com.android.build.api.variant.HostTestBuilder
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.android.build.api.variant.LibraryVariant
import com.android.build.api.variant.ScopedArtifacts
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import ru.solrudev.ackpine.gradle.helpers.withDebugBuildType
import ru.solrudev.ackpine.gradle.tasks.AckpineJacocoReportTask

/**
 * Configures JaCoCo report generation for Ackpine Android library modules.
 */
public class AckpineJacocoPlugin : Plugin<Project> {

	override fun apply(target: Project): Unit = target.run {
		pluginManager.apply("jacoco")
		configureJacoco()
		configureTestTask()
		pluginManager.withPlugin("com.android.library") {
			configureAndroid()
			configureAndroidComponents()
		}
	}

	@Suppress("NewApi")
	private fun Project.configureJacoco() = extensions.configure<JacocoPluginExtension> {
		toolVersion = extensions
			.findByType<VersionCatalogsExtension>()
			?.named("libs")
			?.findVersion("jacoco")
			?.get()
			?.displayName
			.orEmpty()
		reportsDirectory = layout.buildDirectory.dir("jacocoReports")
	}

	private fun Project.configureAndroid() = extensions.configure<LibraryExtension> {
		buildTypes.named("debug") {
			enableUnitTestCoverage = true
			enableAndroidTestCoverage = true
		}
	}

	private fun Project.configureTestTask() = tasks.withType<Test>().configureEach {
		extensions.configure<JacocoTaskExtension> {
			isIncludeNoLocationClasses = true
			excludes = listOf("jdk.internal.*")
		}
	}

	private fun Project.configureAndroidComponents() = extensions.configure<LibraryAndroidComponentsExtension> {
		onVariants(withDebugBuildType()) { variant ->
			val jacocoConfig = registerJacocoReportTask(variant) ?: return@onVariants
			configureConnectedAndroidTest(variant, jacocoConfig)
			finalizeDsl { libraryExtension ->
				configureManagedDevices(libraryExtension, variant, jacocoConfig)
			}
			configureCleanTask(jacocoConfig.task)
		}
	}

	private fun Project.configureManagedDevices(
		libraryExtension: LibraryExtension,
		variant: LibraryVariant,
		jacocoConfig: JacocoConfig
	) = libraryExtension.testOptions.managedDevices {
		val providerFactory = providers
		groups.configureEach {
			val taskName = variant.computeTaskName(action = "${name}Group", subject = "androidTest")
			tasks.matching { it.name == taskName }.configureEach {
				jacocoConfig.androidTestExecutionData.fromCoverageTask(this, providerFactory)
				finalizedBy(jacocoConfig.task)
			}
		}
		localDevices.configureEach {
			val taskName = variant.computeTaskName(action = name, subject = "androidTest")
			tasks.matching { it.name == taskName }.configureEach {
				jacocoConfig.androidTestExecutionData.fromCoverageTask(this, providerFactory)
				finalizedBy(jacocoConfig.task)
			}
		}
	}

	private fun Project.configureConnectedAndroidTest(
		variant: LibraryVariant,
		jacocoConfig: JacocoConfig
	) {
		val providerFactory = providers
		val connectedTaskName = variant.computeTaskName(action = "connected", subject = "androidTest")
		tasks.matching { it.name == connectedTaskName }.configureEach {
			jacocoConfig.androidTestExecutionData.fromCoverageTask(this, providerFactory)
			finalizedBy(jacocoConfig.task)
		}
	}

	private fun Project.registerJacocoReportTask(variant: LibraryVariant): JacocoConfig? {
		val hostTest = variant.hostTests[HostTestBuilder.UNIT_TEST_TYPE]
		val androidTest = variant.deviceTests[DeviceTestBuilder.ANDROID_TEST_TYPE]
		if (hostTest == null && androidTest == null) {
			return null
		}
		val sources = files(variant.sources.java?.all, variant.sources.kotlin?.all)
		val hostTestExecutionData = objects.fileCollection()
		val androidTestExecutionData = objects.fileCollection()
		val reportTaskName = variant.computeTaskName(action = "jacoco", subject = "testReport")
		val reportTaskProvider = tasks.registerJacocoReportTask(
			reportTaskName,
			sources,
			hostTestExecutionData,
			androidTestExecutionData
		)
		hostTest?.configureTestTask { testTask ->
			hostTestExecutionData.from(testTask.extensions.getByType<JacocoTaskExtension>().destinationFile)
			testTask.finalizedBy(reportTaskProvider)
		}
		variant.artifacts
			.forScope(ScopedArtifacts.Scope.PROJECT)
			.use(reportTaskProvider)
			.toGet(
				ScopedArtifact.CLASSES,
				AckpineJacocoReportTask::classJars,
				AckpineJacocoReportTask::classDirs
			)
		return JacocoConfig(reportTaskProvider, androidTestExecutionData)
	}

	private fun TaskContainer.registerJacocoReportTask(
		name: String,
		sources: FileCollection,
		hostTestExecutionData: FileCollection,
		androidTestExecutionData: FileCollection
	) = register<AckpineJacocoReportTask>(name) {
		group = LifecycleBasePlugin.VERIFICATION_GROUP
		description = "Generates JaCoCo coverage reports for unit and instrumentation tests."
		classDirectories.setFrom(
			classDirs.map { dirs ->
				dirs.map { dir ->
					dir.asFileTree.matching {
						exclude(excludePatterns)
					}
				}
			},
			classJars.map { jars ->
				jars.map { jar ->
					archiveOperations.zipTree(jar).matching {
						exclude(excludePatterns)
					}
				}
			}
		)
		sourceDirectories.setFrom(sources)
		additionalSourceDirs.setFrom(sources)
		executionData.from(hostTestExecutionData, androidTestExecutionData)
		reports {
			xml.required = true
			html.required = true
			csv.required = false
		}
	}

	private fun Project.configureCleanTask(task: TaskProvider<*>) = tasks.named<Delete>("clean") {
		delete(task)
	}

	private fun ConfigurableFileCollection.fromCoverageTask(
		task: Task,
		providerFactory: ProviderFactory
	) = from(
		providerFactory.provider {
			task.outputs.files.asFileTree.matching {
				include("**/coverage.ec", "**/*.ec", "**/*.exec")
			}
		}
	)

	private data class JacocoConfig(
		val task: TaskProvider<AckpineJacocoReportTask>,
		val androidTestExecutionData: ConfigurableFileCollection
	)
}

private val excludePatterns = listOf(
	"**/R.class",
	"**/R$*.class",
	"**/BuildConfig.*",
	"**/Manifest*.*",
	"**/*Test*.*",
	"android/**/*.*",
	"**/*_Impl*"
)