/*
 * Copyright (C) 2024 Ilya Fomichev
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

package ru.solrudev.ackpine.gradle.helpers

import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE

/**
 * Adds an outgoing [artifact] to the configuration returned by this provider.
 */
internal fun NamedDomainObjectProvider<out Configuration>.addOutgoingArtifact(artifact: Any) {
	configure {
		outgoing.artifact(artifact)
	}
}

/**
 * Sets a [LIBRARY_ELEMENTS_ATTRIBUTE] attribute value.
 */
internal fun Configuration.libraryElements(value: LibraryElements) {
	attributes {
		attribute(LIBRARY_ELEMENTS_ATTRIBUTE, value)
	}
}