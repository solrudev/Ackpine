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

@file:Suppress("ConstPropertyName", "Unused")

package ru.solrudev.ackpine.installer.parameters

import android.content.pm.PackageInstaller
import android.os.Build
import java.io.Serializable
import kotlin.time.Duration

/**
 * A class to encapsulate constraints for installation.
 *
 * Specifies the conditions to check against for the packages being installed. This can be used
 * by app stores to deliver auto updates without disrupting the user experience (referred as
 * gentle update) - for example, an app store might hold off updates when it find out the
 * app to update is interacting with the user.
 *
 * Takes effect only on API level >= [34][Build.VERSION_CODES.UPSIDE_DOWN_CAKE] with [InstallerType.SESSION_BASED]
 * installer type.
 *
 * @see PackageInstaller.InstallConstraints
 */
public class InstallConstraints private constructor(

	/**
	 * This constraint requires the app in question is not in the foreground.
	 */
	public val isAppNotForegroundRequired: Boolean,

	/**
	 * This constraint requires the app in question is not interacting with the user.
	 * User interaction includes:
	 * * playing or recording audio/video
	 * * sending or receiving network data
	 * * being visible to the user
	 */
	public val isAppNotInteractingRequired: Boolean,

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
	public val isAppNotTopVisibleRequired: Boolean,

	/**
	 * This constraint requires the device is idle.
	 */
	public val isDeviceIdleRequired: Boolean,

	/**
	 * This constraint requires there is no ongoing call in the device.
	 */
	public val isNotInCallRequired: Boolean,

	/**
	 * The maximum time to wait, in milliseconds until the constraints are satisfied.
	 */
	public val timeoutMillis: Long,

	/**
	 * Strategy for handling timeout when the constraints were not satisfied.
	 *
	 * Default strategy is [TimeoutStrategy.Fail].
	 */
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

	/**
	 * Strategy for handling timeout when the constraints for installation were not satisfied.
	 */
	public sealed interface TimeoutStrategy : Serializable {

		/**
		 * Tells installer to commit session immediately after timeout even if constraints are not met.
		 */
		public data object CommitEagerly : TimeoutStrategy {
			private const val serialVersionUID = 6543830064438769365L
			private fun readResolve(): Any = CommitEagerly
		}

		/**
		 * Tells installer to report failure on timeout if constraints are not met.
		 */
		public data object Fail : TimeoutStrategy {
			private const val serialVersionUID = 7548970614475805450L
			private fun readResolve(): Any = Fail
		}

		/**
		 * Tells installer to retry waiting for constraints to be satisfied with the same timeout for [retries] times
		 * if constraints were not met after the first attempt.
		 *
		 * If constraints are met earlier, session will be committed immediately.
		 */
		public data class Retry(public val retries: Int) : TimeoutStrategy {
			private companion object {
				private const val serialVersionUID = -8122854334695670099L
			}
		}

		@Suppress("Unused")
		private data object NonExhaustiveWhenGuard : TimeoutStrategy {
			private const val serialVersionUID = -8649947530739529521L
			private fun readResolve(): Any = NonExhaustiveWhenGuard
		}

		@Suppress("RedundantVisibilityModifier")
		private companion object {

			/**
			 * Tells installer to commit session immediately after timeout even if constraints are not met.
			 */
			@JvmField
			public val COMMIT_EAGERLY: TimeoutStrategy = CommitEagerly

			/**
			 * Tells installer to report failure on timeout if constraints are not met.
			 */
			@JvmField
			public val FAIL: TimeoutStrategy = Fail
		}
	}

	/**
	 * Builder for [InstallConstraints].
	 */
	public class Builder(private val timeoutMillis: Long) {

		/**
		 * This constraint requires the app in question is not in the foreground.
		 */
		public var isAppNotForegroundRequired: Boolean = false
			private set

		/**
		 * This constraint requires the app in question is not interacting with the user.
		 * User interaction includes:
		 * * playing or recording audio/video
		 * * sending or receiving network data
		 * * being visible to the user
		 */
		public var isAppNotInteractingRequired: Boolean = false
			private set

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
		public var isAppNotTopVisibleRequired: Boolean = false
			private set

		/**
		 * This constraint requires the device is idle.
		 */
		public var isDeviceIdleRequired: Boolean = false
			private set

		/**
		 * This constraint requires there is no ongoing call in the device.
		 */
		public var isNotInCallRequired: Boolean = false
			private set

		/**
		 * Strategy for handling timeout when the constraints were not satisfied.
		 *
		 * Default strategy is [TimeoutStrategy.Fail].
		 */
		public var timeoutStrategy: TimeoutStrategy = TimeoutStrategy.Fail
			private set

		/**
		 * Sets [InstallConstraints.isAppNotForegroundRequired].
		 */
		public fun setAppNotForegroundRequired(value: Boolean): Builder = apply {
			isAppNotForegroundRequired = value
		}

		/**
		 * Sets [InstallConstraints.isAppNotInteractingRequired].
		 */
		public fun setAppNotInteractingRequired(value: Boolean): Builder = apply {
			isAppNotInteractingRequired = value
		}

		/**
		 * Sets [InstallConstraints.isAppNotTopVisibleRequired].
		 */
		public fun setAppNotTopVisibleRequired(value: Boolean): Builder = apply {
			isAppNotTopVisibleRequired = value
		}

		/**
		 * Sets [InstallConstraints.isDeviceIdleRequired].
		 */
		public fun setDeviceIdleRequired(value: Boolean): Builder = apply {
			isDeviceIdleRequired = value
		}

		/**
		 * Sets [InstallConstraints.isNotInCallRequired].
		 */
		public fun setNotInCallRequired(value: Boolean): Builder = apply {
			isNotInCallRequired = value
		}

		/**
		 * Sets [InstallConstraints.timeoutStrategy].
		 */
		public fun setTimeoutStrategy(strategy: TimeoutStrategy): Builder = apply {
			timeoutStrategy = strategy
		}

		/**
		 * Constructs a new instance of [InstallConstraints].
		 */
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

		/**
		 * Default [InstallConstraints], which is no constraints.
		 */
		@JvmField
		public val NONE: InstallConstraints = Builder(timeoutMillis = 0L).build()

		/**
		 * Preset constraints suitable for gentle update.
		 */
		@JvmStatic
		public fun gentleUpdate(timeoutMillis: Long, timeoutStrategy: TimeoutStrategy): InstallConstraints {
			return Builder(timeoutMillis)
				.setAppNotInteractingRequired(true)
				.setTimeoutStrategy(timeoutStrategy)
				.build()
		}

		/**
		 * Preset constraints suitable for gentle update.
		 */
		@JvmStatic
		public fun gentleUpdate(timeoutMillis: Long): InstallConstraints {
			return Builder(timeoutMillis)
				.setAppNotInteractingRequired(true)
				.build()
		}

		/**
		 * Preset constraints suitable for gentle update.
		 */
		@JvmSynthetic
		public fun gentleUpdate(timeout: Duration, timeoutStrategy: TimeoutStrategy): InstallConstraints {
			return Builder(timeout.inWholeMilliseconds)
				.setAppNotInteractingRequired(true)
				.setTimeoutStrategy(timeoutStrategy)
				.build()
		}

		/**
		 * Preset constraints suitable for gentle update.
		 */
		@JvmSynthetic
		public fun gentleUpdate(timeout: Duration): InstallConstraints {
			return Builder(timeout.inWholeMilliseconds)
				.setAppNotInteractingRequired(true)
				.build()
		}
	}
}