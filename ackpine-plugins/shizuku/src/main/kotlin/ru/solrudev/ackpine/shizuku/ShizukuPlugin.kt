/*
 * Copyright (C) 2025 Ilya Fomichev
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

package ru.solrudev.ackpine.shizuku

import android.content.pm.PackageInstaller
import rikka.shizuku.Shizuku
import ru.solrudev.ackpine.DelicateAckpineApi
import ru.solrudev.ackpine.installer.parameters.InstallParameters
import ru.solrudev.ackpine.installer.parameters.InstallPreapproval
import ru.solrudev.ackpine.installer.parameters.InstallerType
import ru.solrudev.ackpine.installer.parameters.InstallerType.INTENT_BASED
import ru.solrudev.ackpine.plugability.AckpinePlugin

/**
 * Ackpine plugin which enables installation through Shizuku when applied.
 *
 * Shizuku versions below 11 are not supported, and with these versions installations will fall back to normal system's
 * [PackageInstaller], or [INTENT_BASED] installer (if was set).
 *
 * **Note:** Shizuku permission and binder lifecycle are not managed by this Ackpine plugin. You must handle these in
 * your app to successfully use Shizuku.
 */
public class ShizukuPlugin private constructor() : AckpinePlugin<ShizukuPlugin.Parameters> {

	override val id: String = PLUGIN_ID

	@OptIn(DelicateAckpineApi::class)
	override fun apply(builder: InstallParameters.Builder) {
		if (Shizuku.isPreV11()) {
			return
		}
		builder.apply {
			setInstallerType(InstallerType.SESSION_BASED)
			setRequireUserAction(false)
			setPreapproval(InstallPreapproval.NONE)
		}
	}

	/**
	 * Parameters for [ShizukuPlugin].
	 */
	public class Parameters private constructor(

		/**
		 * Flag to bypass the low target SDK version block for this install.
		 */
		public val bypassLowTargetSdkBlock: Boolean,

		/**
		 * Flag to indicate that you want to allow test packages (those that have set android:testOnly in their
		 * manifest) to be installed.
		 */
		public val allowTest: Boolean,

		/**
		 * Flag to indicate that you want to replace an already installed package, if one exists.
		 */
		public val replaceExisting: Boolean,

		/**
		 * Flag to indicate that an upgrade to a lower version of a package than currently installed has been requested.
		 */
		public val requestDowngrade: Boolean,

		/**
		 * Flag parameter for package install to indicate that all requested permissions should be granted to the
		 * package. If [allUsers] is set the runtime permissions will be granted to all users, otherwise only to the
		 * owner.
		 */
		public val grantAllRequestedPermissions: Boolean,

		/**
		 * Flag to indicate that this install should immediately be visible to all users.
		 */
		public val allUsers: Boolean
	) : AckpinePlugin.Parameters {

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false
			other as Parameters
			if (bypassLowTargetSdkBlock != other.bypassLowTargetSdkBlock) return false
			if (allowTest != other.allowTest) return false
			if (replaceExisting != other.replaceExisting) return false
			if (requestDowngrade != other.requestDowngrade) return false
			if (grantAllRequestedPermissions != other.grantAllRequestedPermissions) return false
			if (allUsers != other.allUsers) return false
			return true
		}

		override fun hashCode(): Int {
			var result = bypassLowTargetSdkBlock.hashCode()
			result = 31 * result + allowTest.hashCode()
			result = 31 * result + replaceExisting.hashCode()
			result = 31 * result + requestDowngrade.hashCode()
			result = 31 * result + grantAllRequestedPermissions.hashCode()
			result = 31 * result + allUsers.hashCode()
			return result
		}

		override fun toString(): String {
			return "Parameters(" +
					"bypassLowTargetSdkBlock=$bypassLowTargetSdkBlock, " +
					"allowTest=$allowTest, " +
					"replaceExisting=$replaceExisting, " +
					"requestDowngrade=$requestDowngrade, " +
					"grantAllRequestedPermissions=$grantAllRequestedPermissions, " +
					"allUsers=$allUsers" +
					")"
		}

		/**
		 * Builder for [ShizukuPlugin.Parameters].
		 */
		public class Builder {

			/**
			 * Flag to bypass the low target SDK version block for this install.
			 */
			public var bypassLowTargetSdkBlock: Boolean = false
				private set

			/**
			 * Flag to indicate that you want to allow test packages (those that have set android:testOnly in their
			 * manifest) to be installed.
			 */
			public var allowTest: Boolean = false
				private set

			/**
			 * Flag to indicate that you want to replace an already installed package, if one exists.
			 */
			public var replaceExisting: Boolean = false
				private set

			/**
			 * Flag to indicate that an upgrade to a lower version of a package than currently installed has been
			 * requested.
			 */
			public var requestDowngrade: Boolean = false
				private set

			/**
			 * Flag parameter for package install to indicate that all requested permissions should be granted to the
			 * package. If [allUsers] is set the runtime permissions will be granted to all users, otherwise only to the
			 * owner.
			 */
			public var grantAllRequestedPermissions: Boolean = false
				private set

			/**
			 * Flag to indicate that this install should immediately be visible to all users.
			 */
			public var allUsers: Boolean = false
				private set

			/**
			 * Sets [ShizukuPlugin.Parameters.bypassLowTargetSdkBlock].
			 */
			public fun setBypassLowTargetSdkBlock(value: Boolean): Builder = apply {
				bypassLowTargetSdkBlock = value
			}

			/**
			 * Sets [ShizukuPlugin.Parameters.allowTest].
			 */
			public fun setAllowTest(value: Boolean): Builder = apply {
				allowTest = value
			}

			/**
			 * Sets [ShizukuPlugin.Parameters.replaceExisting].
			 */
			public fun setReplaceExisting(value: Boolean): Builder = apply {
				replaceExisting = value
			}

			/**
			 * Sets [ShizukuPlugin.Parameters.requestDowngrade].
			 */
			public fun setRequestDowngrade(value: Boolean): Builder = apply {
				requestDowngrade = value
			}

			/**
			 * Sets [ShizukuPlugin.Parameters.grantAllRequestedPermissions].
			 */
			public fun setGrantAllRequestedPermissions(value: Boolean): Builder = apply {
				grantAllRequestedPermissions = value
			}

			/**
			 * Sets [ShizukuPlugin.Parameters.allUsers].
			 */
			public fun setAllUsers(value: Boolean): Builder = apply {
				allUsers = value
			}

			/**
			 * Constructs a new instance of [ShizukuPlugin.Parameters].
			 */
			public fun build(): Parameters = Parameters(
				bypassLowTargetSdkBlock,
				allowTest,
				replaceExisting,
				requestDowngrade,
				grantAllRequestedPermissions,
				allUsers
			)
		}

		@Suppress("Unused")
		private companion object {
			private const val serialVersionUID: Long = 3253616568184160735L
		}
	}

	internal companion object {
		@JvmSynthetic
		internal const val PLUGIN_ID = "ru.solrudev.ackpine.shizuku.ShizukuPlugin"
	}
}