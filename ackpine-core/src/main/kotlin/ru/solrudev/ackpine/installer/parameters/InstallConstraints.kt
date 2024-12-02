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

public class InstallConstraints private constructor(
	public val isAppNotForegroundRequired: Boolean,
	public val isAppNotInteractingRequired: Boolean,
	public val isAppNotTopVisibleRequired: Boolean,
	public val isDeviceIdleRequired: Boolean,
	public val isNotInCallRequired: Boolean,
	public val timeout: Duration
) {

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false
		other as InstallConstraints
		if (isAppNotForegroundRequired != other.isAppNotForegroundRequired) return false
		if (isAppNotInteractingRequired != other.isAppNotInteractingRequired) return false
		if (isAppNotTopVisibleRequired != other.isAppNotTopVisibleRequired) return false
		if (isDeviceIdleRequired != other.isDeviceIdleRequired) return false
		if (isNotInCallRequired != other.isNotInCallRequired) return false
		if (timeout != other.timeout) return false
		return true
	}

	override fun hashCode(): Int {
		var result = isAppNotForegroundRequired.hashCode()
		result = 31 * result + isAppNotInteractingRequired.hashCode()
		result = 31 * result + isAppNotTopVisibleRequired.hashCode()
		result = 31 * result + isDeviceIdleRequired.hashCode()
		result = 31 * result + isNotInCallRequired.hashCode()
		result = 31 * result + timeout.hashCode()
		return result
	}

	override fun toString(): String {
		return "InstallConstraints(" +
				"isAppNotForegroundRequired=$isAppNotForegroundRequired, " +
				"isAppNotInteractingRequired=$isAppNotInteractingRequired, " +
				"isAppNotTopVisibleRequired=$isAppNotTopVisibleRequired, " +
				"isDeviceIdleRequired=$isDeviceIdleRequired, " +
				"isNotInCallRequired=$isNotInCallRequired, " +
				"timeout=$timeout" +
				")"
	}

	public class Builder(timeout: Duration) {

		public var isAppNotForegroundRequired: Boolean = false
			private set

		public var isAppNotInteractingRequired: Boolean = false
			private set

		public var isAppNotTopVisibleRequired: Boolean = false
			private set

		public var isDeviceIdleRequired: Boolean = false
			private set

		public var isNotInCallRequired: Boolean = false
			private set

		public var timeout: Duration = timeout
			private set

		public fun setAppNotForegroundRequired(value: Boolean): Builder = apply {
			isAppNotForegroundRequired = value
		}

		public fun setAppNotInteractingRequired(value: Boolean): Builder = apply {
			isAppNotInteractingRequired = value
		}

		public fun setAppNotTopVisibleRequired(value: Boolean): Builder = apply {
			isAppNotTopVisibleRequired = value
		}

		public fun setDeviceIdleRequired(value: Boolean): Builder = apply {
			isDeviceIdleRequired = value
		}

		public fun setNotInCallRequired(value: Boolean): Builder = apply {
			isNotInCallRequired = value
		}

		public fun setTimeout(timeout: Duration): Builder = apply {
			this.timeout = timeout
		}

		public fun build(): InstallConstraints = InstallConstraints(
			isAppNotForegroundRequired,
			isAppNotInteractingRequired,
			isAppNotTopVisibleRequired,
			isDeviceIdleRequired,
			isNotInCallRequired,
			timeout
		)
	}

	public companion object {

		@JvmField
		public val NONE: InstallConstraints = Builder(timeout = Duration.ZERO).build()

		@JvmStatic
		public fun gentleUpdate(timeout: Duration): InstallConstraints {
			return Builder(timeout).setAppNotInteractingRequired(true).build()
		}
	}
}