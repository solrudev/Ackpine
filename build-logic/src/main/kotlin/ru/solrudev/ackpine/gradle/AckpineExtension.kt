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

package ru.solrudev.ackpine.gradle

import com.android.build.gradle.LibraryExtension
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

public abstract class AckpineExtension @Inject constructor(
	private val libraryExtension: LibraryExtension
) : ExtensionAware {

	private var _id = ""

	/**
	 * Ackpine library ID used in namespace of the generated R and BuildConfig classes and in artifact ID.
	 */
	public var id: String
		get() = _id
		set(value) {
			_id = value
			libraryExtension.namespace = "${Constants.PACKAGE_NAME}.$value"
		}

	/**
	 * Minimum SDK version.
	 */
	public var minSdk: Int? by libraryExtension.defaultConfig::minSdk
}

public open class AckpineArtifact @Inject constructor(objectFactory: ObjectFactory) {

	/**
	 * Name of the published artifact.
	 */
	public val name: Property<String> = objectFactory.property<String>().convention("")

	/**
	 * Enable or disable API documentation generation with Dokka for this module.
	 */
	public val dokka: Property<Boolean> = objectFactory.property<Boolean>().convention(true)
}