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

import com.android.build.api.variant.Component
import com.android.build.api.variant.ComponentBuilder
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.android.build.api.variant.Sources
import com.android.build.api.variant.VariantSelector
import org.gradle.api.Action
import javax.inject.Inject

/**
 * Extension for Ackpine `asset-app-artifacts` plugin.
 */
public abstract class AssetAppArtifactsConsumerExtension @Inject constructor(
	private val androidComponentsExtension: LibraryAndroidComponentsExtension
) {

	private val componentsSelectionListeners = mutableSetOf<ComponentsSelectionListener>()
	private var componentsSelected = false

	/**
	 * Configures [Components][Component] whose [assets][Sources.assets] will be provided with app artifacts.
	 */
	public fun components(
		variantSelector: VariantSelector = selector().all(),
		action: Action<LibraryComponentSelector>
	) {
		check(!componentsSelected) {
			"AssetAppArtifactsConsumerExtension.components() can only be called once."
		}
		componentsSelected = true
		val selection = ComponentsSelection(variantSelector, action)
		for (listener in componentsSelectionListeners) {
			listener(selection)
		}
	}

	/**
	 * Creates a [VariantSelector] instance that can be configured to reduce the set of [ComponentBuilder] instances
	 * participating in the `onVariants` callback invocation.
	 *
	 * @return [VariantSelector] to select the variants of interest.
	 */
	public fun selector(): VariantSelector = androidComponentsExtension.selector()

	/**
	 * Returns a [VariantSelector] for variants with `release` build type.
	 */
	public fun withReleaseBuildType(): VariantSelector {
		return selector().withBuildType("release")
	}

	/**
	 * Returns a [VariantSelector] for variants with `debug` build type.
	 */
	public fun withDebugBuildType(): VariantSelector {
		return selector().withBuildType("debug")
	}

	internal fun addComponentsSelectionListener(listener: ComponentsSelectionListener) {
		componentsSelectionListeners += listener
	}
}

internal typealias ComponentsSelectionListener = (ComponentsSelection) -> Unit

internal data class ComponentsSelection(
	val selector: VariantSelector,
	val action: Action<LibraryComponentSelector>
)