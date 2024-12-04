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

import ru.solrudev.ackpine.session.parameters.SessionParametersDsl
import kotlin.time.Duration

/**
 * Constructs a new instance of [InstallParameters].
 */
public inline fun InstallConstraints(
	timeout: Duration,
	configure: InstallConstraintsDsl.() -> Unit
): InstallConstraints {
	return InstallConstraintsDslBuilder(timeout).apply(configure).build()
}

@SessionParametersDsl
public interface InstallConstraintsDsl {
	public var isAppNotForegroundRequired: Boolean
	public var isAppNotInteractingRequired: Boolean
	public var isAppNotTopVisibleRequired: Boolean
	public var isDeviceIdleRequired: Boolean
	public var isNotInCallRequired: Boolean
	public var timeoutStrategy: InstallConstraints.TimeoutStrategy
}

@PublishedApi
internal class InstallConstraintsDslBuilder(timeout: Duration) : InstallConstraintsDsl {

	private val builder = InstallConstraints.Builder(timeout.inWholeMilliseconds)

	override var isAppNotForegroundRequired: Boolean
		get() = builder.isAppNotForegroundRequired
		set(value) {
			builder.setAppNotForegroundRequired(value)
		}

	override var isAppNotInteractingRequired: Boolean
		get() = builder.isAppNotInteractingRequired
		set(value) {
			builder.setAppNotInteractingRequired(value)
		}

	override var isAppNotTopVisibleRequired: Boolean
		get() = builder.isAppNotTopVisibleRequired
		set(value) {
			builder.setAppNotTopVisibleRequired(value)
		}

	override var isDeviceIdleRequired: Boolean
		get() = builder.isDeviceIdleRequired
		set(value) {
			builder.setDeviceIdleRequired(value)
		}

	override var isNotInCallRequired: Boolean
		get() = builder.isNotInCallRequired
		set(value) {
			builder.setNotInCallRequired(value)
		}

	override var timeoutStrategy: InstallConstraints.TimeoutStrategy
		get() = builder.timeoutStrategy
		set(value) {
			builder.setTimeoutStrategy(value)
		}

	fun build() = builder.build()
}