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

/**
 * Returns a value assigned to the [key] and selected with [valueSelector] from a String map, and throws if key is not
 * found or is empty.
 */
internal inline fun getOrThrow(key: String, valueSelector: (key: String) -> String?): String {
	val value = valueSelector(key)
	check(!value.isNullOrEmpty()) { "$key was not provided" }
	return value
}