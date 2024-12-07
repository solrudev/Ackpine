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

import ru.solrudev.ackpine.installer.parameters.InstallConstraints.TimeoutStrategy
import ru.solrudev.ackpine.session.parameters.SessionParametersDsl
import kotlin.time.Duration

/**
 * DSL allowing to configure [installation constraints][InstallConstraints].
 */
@SessionParametersDsl
public interface InstallConstraintsDsl {

	/**
	 * This constraint requires the app in question is not in the foreground.
	 */
	public var isAppNotForegroundRequired: Boolean

	/**
	 * This constraint requires the app in question is not interacting with the user.
	 * User interaction includes:
	 * * playing or recording audio/video
	 * * sending or receiving network data
	 * * being visible to the user
	 */
	public var isAppNotInteractingRequired: Boolean

	/**
	 * This constraint requires the app in question is not top-visible to the user.
	 * A top-visible app is showing UI at the top of the screen that the user is
	 * interacting with.
	 *
	 * Note this constraint is a subset of [isAppNotForegroundRequired]
	 * because a top-visible app is also a foreground app. This is also a subset
	 * of [isAppNotInteractingRequired] because a top-visible app is interacting
	 * with the user.
	 */
	public var isAppNotTopVisibleRequired: Boolean

	/**
	 * This constraint requires the device is idle.
	 */
	public var isDeviceIdleRequired: Boolean

	/**
	 * This constraint requires there is no ongoing call in the device.
	 */
	public var isNotInCallRequired: Boolean

	/**
	 * Strategy for handling timeout when the constraints were not satisfied.
	 *
	 * Default strategy is [TimeoutStrategy.Fail].
	 */
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