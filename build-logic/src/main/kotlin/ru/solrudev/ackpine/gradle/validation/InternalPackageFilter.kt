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

package ru.solrudev.ackpine.gradle.validation

import kotlinx.validation.ApiValidationExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.findByType
import org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension
import org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationExtension
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
import ru.solrudev.ackpine.gradle.helpers.abiValidation

internal sealed interface InternalPackageFilter {

	fun addPackages(packageNames: Iterable<String>)

	companion object {
		internal fun create(project: Project) = when (AbiValidation.resolve(project)) {
			AbiValidation.BCV -> Bcv(lazy { project.extensions.findByType<ApiValidationExtension>() })
			AbiValidation.KGP -> Kgp(lazy { project.extensions.findByType<KotlinBaseExtension>()?.abiValidation })
		}
	}

	private class Bcv(private val apiValidationExtension: Lazy<ApiValidationExtension?>) : InternalPackageFilter {
		override fun addPackages(packageNames: Iterable<String>) {
			apiValidationExtension.value?.run {
				ignoredPackages.addAll(packageNames)
			}
		}
	}

	private class Kgp(private val abiValidationExtension: Lazy<AbiValidationExtension?>) : InternalPackageFilter {
		@OptIn(ExperimentalAbiValidation::class)
		override fun addPackages(packageNames: Iterable<String>) {
			abiValidationExtension.value?.run {
				filters.exclude.byNames.addAll(packageNames.map { "$it.**" })
			}
		}
	}
}