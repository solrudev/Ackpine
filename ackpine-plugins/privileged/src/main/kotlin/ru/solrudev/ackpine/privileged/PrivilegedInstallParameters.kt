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
import ru.solrudev.ackpine.plugability.AckpinePlugin

/**
 * Shared install parameters for privileged Ackpine plugins.
 */
public abstract class PrivilegedInstallParameters protected constructor(snapshot: Snapshot) : AckpinePlugin.Parameters {

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
	 * Installer package for the app. Empty by default, so the calling app package name will be used. Works only on
	 * Android 9+.
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
				"targetUser=$targetUser" +
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
		public val targetUser: TargetUser
	)

	/**
	 * Base builder for privileged install parameters.
	 */
	public abstract class Builder<Params : PrivilegedInstallParameters, Self : Builder<Params, Self>> {

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
		 * Sets [PrivilegedInstallParameters.bypassLowTargetSdkBlock].
		 */
		public open fun setBypassLowTargetSdkBlock(value: Boolean): Self = self().apply {
			bypassLowTargetSdkBlock = value
		}

		/**
		 * Sets [PrivilegedInstallParameters.allowTest].
		 */
		public open fun setAllowTest(value: Boolean): Self = self().apply {
			allowTest = value
		}

		/**
		 * Sets [PrivilegedInstallParameters.replaceExisting].
		 */
		public open fun setReplaceExisting(value: Boolean): Self = self().apply {
			replaceExisting = value
		}

		/**
		 * Sets [PrivilegedInstallParameters.requestDowngrade].
		 */
		public open fun setRequestDowngrade(value: Boolean): Self = self().apply {
			requestDowngrade = value
		}

		/**
		 * Sets [PrivilegedInstallParameters.grantAllRequestedPermissions].
		 */
		public open fun setGrantAllRequestedPermissions(value: Boolean): Self = self().apply {
			grantAllRequestedPermissions = value
		}

		/**
		 * Sets [PrivilegedInstallParameters.allUsers].
		 */
		public open fun setAllUsers(value: Boolean): Self = self().apply {
			allUsers = value
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
			targetUser
		)

		/**
		 * Constructs a new instance of privileged install parameters.
		 */
		public abstract fun build(): Params

		@Suppress("UNCHECKED_CAST")
		private fun self() = this as Self
	}
}