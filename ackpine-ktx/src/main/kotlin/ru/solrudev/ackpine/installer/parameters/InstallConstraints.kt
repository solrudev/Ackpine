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

package ru.solrudev.ackpine.installer.parameters

import kotlin.time.Duration

/**
 * Constructs a new instance of [InstallConstraints].
 * @param timeout the maximum time to wait, in milliseconds until the constraints are satisfied.
 */
public inline fun InstallConstraints(
	timeout: Duration,
	configure: InstallConstraintsDsl.() -> Unit = {}
): InstallConstraints {
	return InstallConstraintsDslBuilder(timeout).apply(configure).build()
}