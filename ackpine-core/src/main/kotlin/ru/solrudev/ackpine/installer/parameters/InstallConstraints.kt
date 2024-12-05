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

@file:Suppress("ConstPropertyName")

package ru.solrudev.ackpine.installer.parameters

import java.io.Serializable
import kotlin.time.Duration

public class InstallConstraints private constructor(
	public val isAppNotForegroundRequired: Boolean,
	public val isAppNotInteractingRequired: Boolean,
	public val isAppNotTopVisibleRequired: Boolean,
	public val isDeviceIdleRequired: Boolean,
	public val isNotInCallRequired: Boolean,
	public val timeoutMillis: Long,
	public val timeoutStrategy: TimeoutStrategy
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
		if (timeoutMillis != other.timeoutMillis) return false
		if (timeoutStrategy != other.timeoutStrategy) return false
		return true
	}

	override fun hashCode(): Int {
		var result = isAppNotForegroundRequired.hashCode()
		result = 31 * result + isAppNotInteractingRequired.hashCode()
		result = 31 * result + isAppNotTopVisibleRequired.hashCode()
		result = 31 * result + isDeviceIdleRequired.hashCode()
		result = 31 * result + isNotInCallRequired.hashCode()
		result = 31 * result + timeoutMillis.hashCode()
		result = 31 * result + timeoutStrategy.hashCode()
		return result
	}

	override fun toString(): String {
		return "InstallConstraints(" +
				"isAppNotForegroundRequired=$isAppNotForegroundRequired, " +
				"isAppNotInteractingRequired=$isAppNotInteractingRequired, " +
				"isAppNotTopVisibleRequired=$isAppNotTopVisibleRequired, " +
				"isDeviceIdleRequired=$isDeviceIdleRequired, " +
				"isNotInCallRequired=$isNotInCallRequired, " +
				"timeoutMillis=$timeoutMillis, " +
				"timeoutStrategy=$timeoutStrategy" +
				")"
	}

	public sealed interface TimeoutStrategy : Serializable {

		public data object CommitEagerly : TimeoutStrategy {
			private const val serialVersionUID = 6543830064438769365L
			private fun readResolve(): Any = CommitEagerly
		}

		public data object Fail : TimeoutStrategy {
			private const val serialVersionUID = 7548970614475805450L
			private fun readResolve(): Any = Fail
		}

		public data class Retry(public val retries: Int) : TimeoutStrategy {
			private companion object {
				private const val serialVersionUID = -8122854334695670099L
			}
		}

		@Suppress("unused")
		private data object NonExhaustiveWhenGuard : TimeoutStrategy {
			private const val serialVersionUID: Long = -8649947530739529521L
			private fun readResolve(): Any = NonExhaustiveWhenGuard
		}

		@Suppress("RedundantVisibilityModifier")
		private companion object {

			@JvmField
			public val COMMIT_EAGERLY: TimeoutStrategy = CommitEagerly

			@JvmField
			public val FAIL: TimeoutStrategy = Fail
		}
	}

	public class Builder(private val timeoutMillis: Long) {

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

		public var timeoutStrategy: TimeoutStrategy = TimeoutStrategy.Fail
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

		public fun setTimeoutStrategy(strategy: TimeoutStrategy): Builder = apply {
			timeoutStrategy = strategy
		}

		public fun build(): InstallConstraints = InstallConstraints(
			isAppNotForegroundRequired,
			isAppNotInteractingRequired,
			isAppNotTopVisibleRequired,
			isDeviceIdleRequired,
			isNotInCallRequired,
			timeoutMillis,
			timeoutStrategy
		)
	}

	public companion object {

		@JvmField
		public val NONE: InstallConstraints = Builder(timeoutMillis = 0L).build()

		@JvmStatic
		public fun gentleUpdate(timeoutMillis: Long, timeoutStrategy: TimeoutStrategy): InstallConstraints {
			return Builder(timeoutMillis)
				.setAppNotInteractingRequired(true)
				.setTimeoutStrategy(timeoutStrategy)
				.build()
		}

		@JvmStatic
		public fun gentleUpdate(timeoutMillis: Long): InstallConstraints {
			return Builder(timeoutMillis)
				.setAppNotInteractingRequired(true)
				.build()
		}

		@JvmSynthetic
		public fun gentleUpdate(timeout: Duration, timeoutStrategy: TimeoutStrategy): InstallConstraints {
			return Builder(timeout.inWholeMilliseconds)
				.setAppNotInteractingRequired(true)
				.setTimeoutStrategy(timeoutStrategy)
				.build()
		}

		@JvmSynthetic
		public fun gentleUpdate(timeout: Duration): InstallConstraints {
			return Builder(timeout.inWholeMilliseconds)
				.setAppNotInteractingRequired(true)
				.build()
		}
	}
}