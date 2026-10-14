/*
 * Copyright (C) 2026 Ilya Fomichev
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

package ru.solrudev.ackpine.privileged

import androidx.annotation.RestrictTo
import ru.solrudev.ackpine.DelicateAckpineApi
import ru.solrudev.ackpine.plugability.AckpinePlugin

/**
 * Shared uninstall parameters for privileged Ackpine plugins.
 */
public abstract class PrivilegedUninstallParameters protected constructor(
	snapshot: Snapshot
) : AckpinePlugin.Parameters {

	private val overriddenFlags = snapshot.overriddenFlags

	/**
	 * Flag parameter to indicate that you don't want to delete the package's data directory.
	 */
	public val keepData: Boolean = snapshot.keepData

	/**
	 * Flag parameter to indicate that you want the package deleted for all users.
	 */
	public val allUsers: Boolean = snapshot.allUsers

	/**
	 * Flag parameter to indicate that a system app should be marked as uninstalled for the [targetUser].
	 *
	 * This does not remove the app from the system partition. For an updated system app, it prevents the update from
	 * being rolled back globally when uninstalling it for the target user.
	 */
	public val systemApp: Boolean = snapshot.systemApp

	/**
	 * Android user targeted by this uninstall session.
	 *
	 * [allUsers] retains Android's native semantics and may make the selected target irrelevant.
	 *
	 * By default, equals to [TargetUser.CURRENT].
	 */
	public val targetUser: TargetUser = snapshot.targetUser

	/**
	 * Raw Android uninstall flags mask, `0` by default. All `Int` bit patterns are accepted.
	 *
	 * The mask is applied _before_ boolean options. Boolean options take precedence for their corresponding
	 * bits, including clearing bits when `false`. The boolean values remain independent of this mask.
	 * Setting the mask replaces the previous supplied mask.
	 *
	 * Setting this value is a **delicate** API: flags availability and behavior may depend on the Android version
	 * and backend permissions. Supplying flags grants no additional privileges.
	 */
	public val uninstallFlags: Int = snapshot.uninstallFlags

	/**
	 * Returns the simple class name used in [toString].
	 */
	protected abstract fun getName(): String

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false
		other as PrivilegedUninstallParameters
		if (keepData != other.keepData) return false
		if (allUsers != other.allUsers) return false
		if (systemApp != other.systemApp) return false
		if (targetUser != other.targetUser) return false
		if (uninstallFlags != other.uninstallFlags) return false
		if (overriddenFlags != other.overriddenFlags) return false
		return true
	}

	override fun hashCode(): Int {
		var result = keepData.hashCode()
		result = 31 * result + allUsers.hashCode()
		result = 31 * result + systemApp.hashCode()
		result = 31 * result + targetUser.hashCode()
		result = 31 * result + uninstallFlags.hashCode()
		result = 31 * result + overriddenFlags
		return result
	}

	override fun toString(): String = "${getName()}(" +
			"keepData=$keepData, " +
			"allUsers=$allUsers, " +
			"systemApp=$systemApp, " +
			"targetUser=$targetUser, " +
			"uninstallFlags=$uninstallFlags" +
			")"

	/**
	 * Immutable privileged uninstall parameter values passed to constructors.
	 */
	@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
	public class Snapshot internal constructor(

		/**
		 * Whether to retain the package data directory.
		 */
		public val keepData: Boolean,

		/**
		 * Whether to uninstall for all users.
		 */
		public val allUsers: Boolean,

		/**
		 * Whether to use the system-app uninstall behavior.
		 */
		public val systemApp: Boolean,

		/**
		 * Android user targeted by the uninstall.
		 */
		public val targetUser: TargetUser,

		/**
		 * Raw Android uninstall flags mask.
		 */
		public val uninstallFlags: Int,

		@get:JvmSynthetic
		internal val overriddenFlags: Int
	)

	/**
	 * Base builder for privileged uninstall parameters.
	 */
	public abstract class Builder<
			Params : PrivilegedUninstallParameters,
			Self : Builder<Params, Self>
			> protected constructor(
		/**
		 * Whether only boolean options set through this builder's boolean setters override platform and raw uninstall
		 * flags. When `false` (default), all boolean options override their corresponding bits, including their default
		 * `false` values. When `true`, options whose setters have not been called leave those bits unchanged.
		 */
		partialOverride: Boolean = false
	) {

		private var overriddenFlags = if (partialOverride) 0 else -1

		/**
		 * Flag parameter to indicate that you don't want to delete the package's data directory.
		 */
		public var keepData: Boolean = false
			protected set

		/**
		 * Flag parameter to indicate that you want the package deleted for all users.
		 */
		public var allUsers: Boolean = false
			protected set

		/**
		 * Flag parameter to indicate that a system app should be marked as uninstalled for the [targetUser].
		 *
		 * This does not remove the app from the system partition. For an updated system app, it prevents the update
		 * from being rolled back globally when uninstalling it for the target user.
		 */
		public var systemApp: Boolean = false
			protected set

		/**
		 * Android user targeted by this uninstall session.
		 *
		 * [allUsers] retains Android's native semantics and may make the selected target irrelevant.
		 *
		 * By default, equals to [TargetUser.CURRENT].
		 */
		public var targetUser: TargetUser = TargetUser.CURRENT
			protected set

		/**
		 * Raw Android uninstall flags mask, `0` by default. All `Int` bit patterns are accepted.
		 *
		 * The mask is applied _before_ boolean options. Boolean options take precedence for their corresponding
		 * bits, including clearing bits when `false`. The boolean values remain independent of this mask.
		 * Setting the mask replaces the previous supplied mask.
		 *
		 * Setting this value is a **delicate** API: flags availability and behavior may depend on the Android version
		 * and backend permissions. Supplying flags grants no additional privileges.
		 */
		public var uninstallFlags: Int = 0
			protected set

		/**
		 * Sets [PrivilegedUninstallParameters.keepData].
		 */
		public open fun setKeepData(value: Boolean): Self = self().apply {
			keepData = value
			overriddenFlags = overriddenFlags or DELETE_KEEP_DATA
		}

		/**
		 * Sets [PrivilegedUninstallParameters.allUsers].
		 */
		public open fun setAllUsers(value: Boolean): Self = self().apply {
			allUsers = value
			overriddenFlags = overriddenFlags or DELETE_ALL_USERS
		}

		/**
		 * Sets [PrivilegedUninstallParameters.systemApp].
		 */
		public open fun setSystemApp(value: Boolean): Self = self().apply {
			systemApp = value
			overriddenFlags = overriddenFlags or DELETE_SYSTEM_APP
		}

		/**
		 * Sets [PrivilegedUninstallParameters.targetUser].
		 */
		public open fun setTargetUser(value: TargetUser): Self = self().apply {
			targetUser = value
		}

		/**
		 * Sets [PrivilegedUninstallParameters.uninstallFlags].
		 *
		 * This is a **delicate** API.
		 */
		@DelicateAckpineApi
		public open fun setUninstallFlags(value: Int): Self = self().apply {
			uninstallFlags = value
		}

		/**
		 * Returns an immutable snapshot of this builder for passing to a [PrivilegedUninstallParameters] constructor.
		 */
		protected fun buildSnapshot(): Snapshot = Snapshot(
			keepData,
			allUsers,
			systemApp,
			targetUser,
			uninstallFlags,
			overriddenFlags
		)

		/**
		 * Constructs a new instance of privileged uninstall parameters.
		 */
		public abstract fun build(): Params

		@Suppress("UNCHECKED_CAST")
		private fun self() = this as Self
	}

	@JvmSynthetic
	internal fun isFlagOverridden(flag: Int): Boolean = overriddenFlags and flag != 0
}