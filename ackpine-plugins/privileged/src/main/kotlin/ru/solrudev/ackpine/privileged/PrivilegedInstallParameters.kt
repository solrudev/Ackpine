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
 * Shared install parameters for privileged Ackpine plugins.
 */
public abstract class PrivilegedInstallParameters protected constructor(snapshot: Snapshot) : AckpinePlugin.Parameters {

	private val overriddenFlags = snapshot.overriddenFlags

	/**
	 * Flag to bypass the low target SDK version block for this install.
	 */
	public val bypassLowTargetSdkBlock: Boolean = snapshot.bypassLowTargetSdkBlock

	/**
	 * Flag to indicate that you want to allow test packages (those that have set android:testOnly in their
	 * manifest) to be installed.
	 */
	public val allowTest: Boolean = snapshot.allowTest

	/**
	 * Flag to indicate that you want to replace an already installed package, if one exists.
	 */
	public val replaceExisting: Boolean = snapshot.replaceExisting

	/**
	 * Flag to indicate that an upgrade to a lower version of a package than currently installed has been requested.
	 */
	public val requestDowngrade: Boolean = snapshot.requestDowngrade

	/**
	 * Flag parameter for package install to indicate that all requested permissions should be granted to the package.
	 * If [allUsers] is set the runtime permissions will be granted to all users, otherwise only to the owner.
	 */
	public val grantAllRequestedPermissions: Boolean = snapshot.grantAllRequestedPermissions

	/**
	 * Flag to indicate that this install should immediately be visible to all users.
	 */
	public val allUsers: Boolean = snapshot.allUsers

	/**
	 * Installer package for the app. Empty by default, so the calling app package name will be used.
	 */
	public val installerPackageName: String = snapshot.installerPackageName

	/**
	 * Android user targeted by this install session.
	 *
	 * [allUsers] retains Android's native semantics and may make the selected target irrelevant.
	 *
	 * By default, equals to [TargetUser.CURRENT].
	 */
	public val targetUser: TargetUser = snapshot.targetUser

	/**
	 * Raw Android install flags mask, `0` by default. All `Int` bit patterns are accepted.
	 *
	 * The mask is ORed with platform flags _before_ boolean options. Boolean options take precedence for their
	 * corresponding bits, including clearing bits when `false`.
	 * The boolean values remain independent of this mask. Setting the mask replaces the previous supplied
	 * mask; unrelated platform flags are preserved.
	 *
	 * Setting this value is a **delicate** API: flags availability and behavior may depend on the Android version
	 * and backend permissions. Supplying flags grants no additional privileges.
	 */
	public val installFlags: Int = snapshot.installFlags

	/**
	 * Returns the simple class name used in [toString].
	 */
	protected abstract fun getName(): String

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false
		other as PrivilegedInstallParameters
		if (bypassLowTargetSdkBlock != other.bypassLowTargetSdkBlock) return false
		if (allowTest != other.allowTest) return false
		if (replaceExisting != other.replaceExisting) return false
		if (requestDowngrade != other.requestDowngrade) return false
		if (grantAllRequestedPermissions != other.grantAllRequestedPermissions) return false
		if (allUsers != other.allUsers) return false
		if (installerPackageName != other.installerPackageName) return false
		if (targetUser != other.targetUser) return false
		if (installFlags != other.installFlags) return false
		if (overriddenFlags != other.overriddenFlags) return false
		return true
	}

	override fun hashCode(): Int {
		var result = bypassLowTargetSdkBlock.hashCode()
		result = 31 * result + allowTest.hashCode()
		result = 31 * result + replaceExisting.hashCode()
		result = 31 * result + requestDowngrade.hashCode()
		result = 31 * result + grantAllRequestedPermissions.hashCode()
		result = 31 * result + allUsers.hashCode()
		result = 31 * result + installerPackageName.hashCode()
		result = 31 * result + targetUser.hashCode()
		result = 31 * result + installFlags.hashCode()
		result = 31 * result + overriddenFlags
		return result
	}

	override fun toString(): String {
		return "${getName()}(" +
				"bypassLowTargetSdkBlock=$bypassLowTargetSdkBlock, " +
				"allowTest=$allowTest, " +
				"replaceExisting=$replaceExisting, " +
				"requestDowngrade=$requestDowngrade, " +
				"grantAllRequestedPermissions=$grantAllRequestedPermissions, " +
				"allUsers=$allUsers, " +
				"installerPackageName=$installerPackageName, " +
				"targetUser=$targetUser, " +
				"installFlags=$installFlags" +
				")"
	}

	/**
	 * Immutable privileged install parameter values passed to constructors.
	 */
	@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
	public class Snapshot internal constructor(

		/**
		 * Whether to bypass the low target SDK version block.
		 */
		public val bypassLowTargetSdkBlock: Boolean,

		/**
		 * Whether to allow test-only packages.
		 */
		public val allowTest: Boolean,

		/**
		 * Whether to replace an existing package.
		 */
		public val replaceExisting: Boolean,

		/**
		 * Whether to request a downgrade.
		 */
		public val requestDowngrade: Boolean,

		/**
		 * Whether to grant all requested permissions.
		 */
		public val grantAllRequestedPermissions: Boolean,

		/**
		 * Whether to install for all users.
		 */
		public val allUsers: Boolean,

		/**
		 * Installer package name, or an empty string to use the calling package.
		 */
		public val installerPackageName: String,

		/**
		 * Android user targeted by the install.
		 */
		public val targetUser: TargetUser,

		/**
		 * Raw Android install flags mask.
		 */
		public val installFlags: Int,

		@get:JvmSynthetic
		internal val overriddenFlags: Int
	)

	/**
	 * Base builder for privileged install parameters.
	 */
	public abstract class Builder<
			Params : PrivilegedInstallParameters,
			Self : Builder<Params, Self>
			> protected constructor(
		/**
		 * Whether only boolean options set through this builder's boolean setters override platform and raw install
		 * flags. When `false` (default), all boolean options override their corresponding bits, including their default
		 * `false` values. When `true`, options whose setters have not been called leave those bits unchanged.
		 */
		partialOverride: Boolean = false
	) {

		private var overriddenFlags = if (partialOverride) 0 else -1

		/**
		 * Flag to bypass the low target SDK version block for this install.
		 */
		public var bypassLowTargetSdkBlock: Boolean = false
			protected set

		/**
		 * Flag to indicate that you want to allow test packages (those that have set android:testOnly in their
		 * manifest) to be installed.
		 */
		public var allowTest: Boolean = false
			protected set

		/**
		 * Flag to indicate that you want to replace an already installed package, if one exists.
		 */
		public var replaceExisting: Boolean = false
			protected set

		/**
		 * Flag to indicate that an upgrade to a lower version of a package than currently installed has been requested.
		 */
		public var requestDowngrade: Boolean = false
			protected set

		/**
		 * Flag parameter for package install to indicate that all requested permissions should be granted to the package.
		 * If [allUsers] is set the runtime permissions will be granted to all users, otherwise only to the owner.
		 */
		public var grantAllRequestedPermissions: Boolean = false
			protected set

		/**
		 * Flag to indicate that this install should immediately be visible to all users.
		 */
		public var allUsers: Boolean = false
			protected set

		/**
		 * Installer package for the app. Empty by default, so the calling app package name will be used. Takes effect
		 * only on Android 9+.
		 */
		public var installerPackageName: String = ""
			protected set

		/**
		 * Android user targeted by this install session.
		 *
		 * [allUsers] retains Android's native semantics and may make the selected target irrelevant.
		 *
		 * By default, equals to [TargetUser.CURRENT].
		 */
		public var targetUser: TargetUser = TargetUser.CURRENT
			protected set

		/**
		 * Raw Android install flags mask, `0` by default. All `Int` bit patterns are accepted.
		 *
		 * The mask is ORed with platform flags _before_ boolean options. Boolean options take precedence for their
		 * corresponding bits, including clearing bits when `false`.
		 * The boolean values remain independent of this mask. Setting the mask replaces the previous supplied
		 * mask; unrelated platform flags are preserved.
		 *
		 * Setting this value is a **delicate** API: flags availability and behavior may depend on the Android version
		 * and backend permissions. Supplying flags grants no additional privileges.
		 */
		public var installFlags: Int = 0
			protected set

		/**
		 * Sets [PrivilegedInstallParameters.bypassLowTargetSdkBlock].
		 */
		public open fun setBypassLowTargetSdkBlock(value: Boolean): Self = self().apply {
			bypassLowTargetSdkBlock = value
			overriddenFlags = overriddenFlags or INSTALL_BYPASS_LOW_TARGET_SDK_BLOCK
		}

		/**
		 * Sets [PrivilegedInstallParameters.allowTest].
		 */
		public open fun setAllowTest(value: Boolean): Self = self().apply {
			allowTest = value
			overriddenFlags = overriddenFlags or INSTALL_ALLOW_TEST
		}

		/**
		 * Sets [PrivilegedInstallParameters.replaceExisting].
		 */
		public open fun setReplaceExisting(value: Boolean): Self = self().apply {
			replaceExisting = value
			overriddenFlags = overriddenFlags or INSTALL_REPLACE_EXISTING
		}

		/**
		 * Sets [PrivilegedInstallParameters.requestDowngrade].
		 */
		public open fun setRequestDowngrade(value: Boolean): Self = self().apply {
			requestDowngrade = value
			overriddenFlags = overriddenFlags or INSTALL_REQUEST_DOWNGRADE or INSTALL_ALLOW_DOWNGRADE
		}

		/**
		 * Sets [PrivilegedInstallParameters.grantAllRequestedPermissions].
		 */
		public open fun setGrantAllRequestedPermissions(value: Boolean): Self = self().apply {
			grantAllRequestedPermissions = value
			overriddenFlags = overriddenFlags or INSTALL_GRANT_ALL_REQUESTED_PERMISSIONS
		}

		/**
		 * Sets [PrivilegedInstallParameters.allUsers].
		 */
		public open fun setAllUsers(value: Boolean): Self = self().apply {
			allUsers = value
			overriddenFlags = overriddenFlags or INSTALL_ALL_USERS
		}

		/**
		 * Sets [PrivilegedInstallParameters.installerPackageName].
		 */
		public open fun setInstallerPackageName(value: String): Self = self().apply {
			installerPackageName = value
		}

		/**
		 * Sets [PrivilegedInstallParameters.targetUser].
		 */
		public open fun setTargetUser(value: TargetUser): Self = self().apply {
			targetUser = value
		}

		/**
		 * Sets [PrivilegedInstallParameters.installFlags].
		 *
		 * This is a **delicate** API.
		 */
		@DelicateAckpineApi
		public open fun setInstallFlags(value: Int): Self = self().apply {
			installFlags = value
		}

		/**
		 * Returns an immutable snapshot of this builder for passing to a [PrivilegedInstallParameters] constructor.
		 */
		protected fun buildSnapshot(): Snapshot = Snapshot(
			bypassLowTargetSdkBlock,
			allowTest,
			replaceExisting,
			requestDowngrade,
			grantAllRequestedPermissions,
			allUsers,
			installerPackageName,
			targetUser,
			installFlags,
			overriddenFlags
		)

		/**
		 * Constructs a new instance of privileged install parameters.
		 */
		public abstract fun build(): Params

		@Suppress("UNCHECKED_CAST")
		private fun self() = this as Self
	}

	@JvmSynthetic
	internal fun isFlagOverridden(flag: Int): Boolean = overriddenFlags and flag != 0
}