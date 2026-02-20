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
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.ApplicationVariant
import com.android.build.api.variant.Component
import com.android.build.api.variant.DeviceTestBuilder
import com.android.build.api.variant.HasDeviceTests
import com.android.build.api.variant.HasHostTests
import com.android.build.api.variant.HostTestBuilder
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.android.build.api.variant.LibraryVariant
import com.android.build.api.variant.ScopedArtifacts
import com.android.build.api.variant.Variant
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import ru.solrudev.ackpine.gradle.helpers.withDebugBuildType
import ru.solrudev.ackpine.gradle.tasks.AckpineJacocoReportTask
import kotlin.jvm.optionals.getOrNull

/**
 * Configures JaCoCo report generation for Ackpine Android modules.
 */
public class AckpineJacocoPlugin : Plugin<Project> {

	override fun apply(target: Project): Unit = target.run {
		pluginManager.apply("jacoco")
		configureJacoco()
		configureTestTask()
		pluginManager.withPlugin("com.android.library") {
			configureAndroid<LibraryExtension>()
			configureAndroidComponents<LibraryAndroidComponentsExtension, LibraryVariant>()
		}
		pluginManager.withPlugin("com.android.application") {
			configureAndroid<ApplicationExtension>()
			configureAndroidComponents<ApplicationAndroidComponentsExtension, ApplicationVariant>()
		}
	}

	private fun Project.configureJacoco() = extensions.configure<JacocoPluginExtension> {
		val jacocoVersion = extensions
			.findByType<VersionCatalogsExtension>()
			?.named("libs")
			?.findVersion("jacoco")
			?.getOrNull()
			?.requiredVersion
		if (jacocoVersion != null) {
			toolVersion = jacocoVersion
		}
		reportsDirectory = layout.buildDirectory.dir("jacocoReports")
	}

	private inline fun <reified E : CommonExtension<*, *, *, *, *, *>> Project.configureAndroid() {
		extensions.configure<E> {
			buildTypes.named("debug") {
				enableUnitTestCoverage = true
				enableAndroidTestCoverage = true
			}
		}
	}

	private fun Project.configureTestTask() = tasks.withType<Test>().configureEach {
		extensions.configure<JacocoTaskExtension> {
			isIncludeNoLocationClasses = true
			excludes = listOf("jdk.internal.*")
		}
	}

	private inline fun <reified E : AndroidComponentsExtension<*, *, V>, V> Project.configureAndroidComponents()
			where V : Variant,
				  V : HasHostTests,
				  V : HasDeviceTests {
		extensions.configure<E> {
			onVariants(withDebugBuildType()) { variant ->
				val jacocoConfig = registerJacocoReportTask(variant) ?: return@onVariants
				if (jacocoConfig.hasAndroidTests) {
					configureConnectedAndroidTest(variant, jacocoConfig)
					configureManagedDevices(variant, jacocoConfig)
				}
			}
		}
	}

	private fun Project.configureManagedDevices(
		component: Component,
		jacocoConfig: JacocoConfig
	) = extensions.configure<LibraryExtension> {
		testOptions.managedDevices.allDevices.configureEach {
			configureManagedDeviceReport(component, jacocoConfig.sources, testTaskAction = name)
		}
		testOptions.managedDevices.groups.configureEach {
			configureManagedDeviceReport(component, jacocoConfig.sources, testTaskAction = "${name}Group")
		}
	}

	private fun Project.configureManagedDeviceReport(
		component: Component,
		sources: FileCollection,
		testTaskAction: String
	) {
		val reportTaskAction = "${testTaskAction}Jacoco"
		val testTaskName = component.computeTaskName(action = testTaskAction, subject = "androidTest")
		val reportTask = registerAndroidTestReportTask(component, sources, testTaskName, reportTaskAction)
		tasks.matching { it.name == testTaskName }.configureEach {
			finalizedBy(reportTask)
		}
	}

	private fun Project.configureConnectedAndroidTest(
		component: Component,
		jacocoConfig: JacocoConfig
	) {
		val providerFactory = providers
		val connectedTaskName = component.computeTaskName(action = "connected", subject = "androidTest")
		tasks.matching { it.name == connectedTaskName }.configureEach {
			jacocoConfig.androidTestExecutionData.fromCoverageTask(this, providerFactory)
			finalizedBy(jacocoConfig.task)
		}
	}

	private fun <C> Project.registerJacocoReportTask(component: C): JacocoConfig?
			where C : Component,
				  C : HasHostTests,
				  C : HasDeviceTests {
		val hostTest = component.hostTests[HostTestBuilder.UNIT_TEST_TYPE]
		val androidTest = component.deviceTests[DeviceTestBuilder.ANDROID_TEST_TYPE]
		if (hostTest == null && androidTest == null) {
			return null
		}
		val sources = files(component.sources.java?.all, component.sources.kotlin?.all)
		val hostTestExecutionData = objects.fileCollection()
		val androidTestExecutionData = objects.fileCollection()
		val reportTaskName = component.computeTaskName(action = "jacoco", subject = "testReport")
		val reportTask = tasks.registerJacocoReportTask(
			reportTaskName,
			sources,
			hostTestExecutionData,
			androidTestExecutionData
		)
		hostTest?.configureTestTask { testTask ->
			hostTestExecutionData.from(testTask.extensions.getByType<JacocoTaskExtension>().destinationFile)
			testTask.finalizedBy(reportTask)
		}
		reportTask.wireProjectClasses(component)
		return JacocoConfig(
			hasAndroidTests = androidTest != null,
			reportTask,
			androidTestExecutionData,
			sources
		)
	}

	private fun Project.registerAndroidTestReportTask(
		component: Component,
		sources: FileCollection,
		testTaskName: String,
		reportNameAction: String
	): TaskProvider<AckpineJacocoReportTask> {
		val executionData = objects.fileCollection()
		val executionDataFiles = providers
			.provider { tasks.named(testTaskName) }
			.flatMap { taskProvider ->
				taskProvider.map { task ->
					task.coverageExecutionDataFiles()
				}
			}
		executionData.from(executionDataFiles)
		val reportTaskName = component.computeTaskName(action = reportNameAction, subject = "androidTestReport")
		val reportTask = tasks.registerJacocoReportTask(reportTaskName, sources, executionData)
		reportTask.wireProjectClasses(component)
		return reportTask
	}

	private fun TaskContainer.registerJacocoReportTask(
		name: String,
		sources: FileCollection,
		vararg executionData: FileCollection
	) = register<AckpineJacocoReportTask>(name) {
		group = LifecycleBasePlugin.VERIFICATION_GROUP
		description = "Generates JaCoCo coverage report."
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
		for (data in executionData) {
			this.executionData.from(data)
		}
		reports {
			xml.required = true
			html.required = true
			csv.required = false
		}
	}

	private fun TaskProvider<AckpineJacocoReportTask>.wireProjectClasses(component: Component) {
		component.artifacts
			.forScope(ScopedArtifacts.Scope.PROJECT)
			.use(this)
			.toGet(
				ScopedArtifact.CLASSES,
				AckpineJacocoReportTask::classJars,
				AckpineJacocoReportTask::classDirs
			)
	}

	private fun ConfigurableFileCollection.fromCoverageTask(
		task: Task,
		providerFactory: ProviderFactory
	) = from(
		providerFactory.provider {
			task.coverageExecutionDataFiles()
		}
	)

	private fun Task.coverageExecutionDataFiles() = outputs.files.asFileTree.matching {
		include("**/coverage.ec", "**/*.ec", "**/*.exec")
	}

	private data class JacocoConfig(
		val hasAndroidTests: Boolean,
		val task: TaskProvider<AckpineJacocoReportTask>,
		val androidTestExecutionData: ConfigurableFileCollection,
		val sources: FileCollection
	)
}

private val excludePatterns = listOf(
	"**/R.class",
	"**/R$*.class",
	"**/BuildConfig.*",
	"**/Manifest*.*",
	"**/*Test.*",
	"android/**/*.*",
	"**/*_Impl*"
)