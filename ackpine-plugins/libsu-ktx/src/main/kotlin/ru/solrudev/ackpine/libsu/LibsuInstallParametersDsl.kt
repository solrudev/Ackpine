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

package ru.solrudev.ackpine.libsu

import ru.solrudev.ackpine.session.parameters.SessionParametersDsl

/**
 * DSL allowing to configure [install parameters for LibsuPlugin][LibsuPlugin.InstallParameters].
 */
@SessionParametersDsl
public interface LibsuInstallParametersDsl {

	/**
	 * Flag to bypass the low target SDK version block for this install.
	 */
	public var bypassLowTargetSdkBlock: Boolean

	/**
	 * Flag to indicate that you want to allow test packages (those that have set android:testOnly in their
	 * manifest) to be installed.
	 */
	public var allowTest: Boolean

	/**
	 * Flag to indicate that you want to replace an already installed package, if one exists.
	 */
	public var replaceExisting: Boolean

	/**
	 * Flag to indicate that an upgrade to a lower version of a package than currently installed has been requested.
	 */
	public var requestDowngrade: Boolean

	/**
	 * Flag parameter for package install to indicate that all requested permissions should be granted to the
	 * package. If [allUsers] is set the runtime permissions will be granted to all users, otherwise only to the
	 * owner.
	 */
	public var grantAllRequestedPermissions: Boolean

	/**
	 * Flag to indicate that this install should immediately be visible to all users.
	 */
	public var allUsers: Boolean

	/**
	 * Installer package for the app. Empty by default, so the calling app package name will be used. Takes effect only
	 * on Android 9+.
	 */
	public var installerPackageName: String
}

@PublishedApi
internal class LibsuInstallParametersDslBuilder : LibsuInstallParametersDsl {

	private val builder = LibsuPlugin.InstallParameters.Builder()

	override var bypassLowTargetSdkBlock: Boolean
		get() = builder.bypassLowTargetSdkBlock
		set(value) {
			builder.setBypassLowTargetSdkBlock(value)
		}

	override var allowTest: Boolean
		get() = builder.allowTest
		set(value) {
			builder.setAllowTest(value)
		}

	override var replaceExisting: Boolean
		get() = builder.replaceExisting
		set(value) {
			builder.setReplaceExisting(value)
		}

	override var requestDowngrade: Boolean
		get() = builder.requestDowngrade
		set(value) {
			builder.setRequestDowngrade(value)
		}

	override var grantAllRequestedPermissions: Boolean
		get() = builder.grantAllRequestedPermissions
		set(value) {
			builder.setGrantAllRequestedPermissions(value)
		}

	override var allUsers: Boolean
		get() = builder.allUsers
		set(value) {
			builder.setAllUsers(value)
		}

	override var installerPackageName: String
		get() = builder.installerPackageName
		set(value) {
			builder.setInstallerPackageName(value)
		}

	fun build() = builder.build()
}