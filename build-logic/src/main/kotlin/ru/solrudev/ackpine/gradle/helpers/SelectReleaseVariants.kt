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

import org.gradle.api.DomainObjectSet

/**
 * Returns a collection which contains only release variants/build types.
 * @param nameSelector variant/build type name selector.
 */
public inline fun <T> DomainObjectSet<T>.selectReleaseVariants(
	crossinline nameSelector: (T) -> String
): DomainObjectSet<T> = matching { nameSelector(it).lowercase().endsWith("release") }