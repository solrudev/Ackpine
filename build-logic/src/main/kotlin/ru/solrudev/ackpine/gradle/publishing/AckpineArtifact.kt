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

package ru.solrudev.ackpine.gradle.publishing

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

/**
 * Extension for Ackpine `library-publish` plugin.
 */
public open class AckpineArtifact @Inject constructor(objectFactory: ObjectFactory) {

	/**
	 * Name of the published artifact.
	 */
	public val name: Property<String> = objectFactory.property<String>().convention("")

	/**
	 * Inception year of the published artifact.
	 */
	public val inceptionYear: Property<String> = objectFactory.property<String>().convention("2023")
}