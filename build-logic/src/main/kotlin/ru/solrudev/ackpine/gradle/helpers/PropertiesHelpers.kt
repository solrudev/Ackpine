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

package ru.solrudev.ackpine.gradle.helpers

import java.io.File
import java.util.Properties

/**
 * Returns a [Properties] object read from a file.
 */
@Suppress("UNCHECKED_CAST")
internal fun File.toPropertiesMap(): Map<String, String> {
	val properties = Properties()
	inputStream().use(properties::load)
	return properties as Map<String, String>
}

/**
 * Returns a value assigned to the [key] and throws if key is not found or if value is empty.
 */
internal fun Map<String, String>.getOrThrow(key: String): String {
	val value = get(key)
	check(!value.isNullOrEmpty()) { "$key was not provided" }
	return value
}