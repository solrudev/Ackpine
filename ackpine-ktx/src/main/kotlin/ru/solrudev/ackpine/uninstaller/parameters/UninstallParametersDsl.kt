/*
 * Copyright (C) 2023 Ilya Fomichev
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

package ru.solrudev.ackpine.uninstaller.parameters

import ru.solrudev.ackpine.exceptions.VersionedUninstallNotSupportedException
import ru.solrudev.ackpine.plugability.AckpinePlugin
import ru.solrudev.ackpine.plugability.AckpineUninstallPlugin
import ru.solrudev.ackpine.session.parameters.Confirmation
import ru.solrudev.ackpine.session.parameters.ConfirmationDsl
import ru.solrudev.ackpine.session.parameters.NotificationData
import ru.solrudev.ackpine.session.parameters.SessionParametersDsl
import kotlin.reflect.KClass

/**
 * DSL allowing to configure [parameters for creating uninstall session][UninstallParameters].
 */
@SessionParametersDsl
public interface UninstallParametersDsl : ConfirmationDsl {

	/**
	 * Name of the package to be uninstalled.
	 */
	public var packageName: String

	/**
	 * Version targeted by the uninstall session.
	 *
	 * Default value is [PackageVersionCode.HIGHEST].
	 *
	 * Assignment selects [VersionedUninstallPolicy.STRICT]. Use [packageVersionCode] with an explicit policy to
	 * permit [name-only fallback][VersionedUninstallPolicy.ALLOW_NAME_ONLY].
	 *
	 * @throws VersionedUninstallNotSupportedException if an exact target cannot be represented on the current API.
	 */
	public var packageVersionCode: PackageVersionCode

	/**
	 * Policy configured together with the version through [packageVersionCode].
	 *
	 * Default value is [VersionedUninstallPolicy.STRICT].
	 */
	public val versionedUninstallPolicy: VersionedUninstallPolicy

	/**
	 * Configures the version and [policy] atomically. Omitting [policy] selects [VersionedUninstallPolicy.STRICT].
	 * @param packageVersionCode the version to target, or [PackageVersionCode.HIGHEST] for name-only behavior.
	 * @param policy whether a name-only fallback is acceptable when exact targeting is unavailable.
	 * @throws VersionedUninstallNotSupportedException if [policy] is [VersionedUninstallPolicy.STRICT] and an exact
	 * target is requested on API level < 26, or its code exceeds [Int.MAX_VALUE] on API levels 26–27.
	 * Configuration remains unchanged when validation fails.
	 */
	public fun packageVersionCode(
		packageVersionCode: PackageVersionCode,
		policy: VersionedUninstallPolicy = VersionedUninstallPolicy.STRICT
	)

	/**
	 * Type of the package uninstaller implementation.
	 *
	 * Default value is [UninstallerType.DEFAULT].
	 *
	 * When getting/setting the value of this property, the following invariants are maintained:
	 * * When on API level < 21, [UninstallerType.INTENT_BASED] is always returned/set regardless of the
	 * current/provided value;
	 * * When on API level >= 26 and [packageVersionCode] targets an exact version representable on the current
	 * API level, [UninstallerType.PACKAGE_INSTALLER_BASED] is always returned/set regardless of the
	 * current/provided value. On API levels 26–27, the code must not exceed [Int.MAX_VALUE].
	 */
	public var uninstallerType: UninstallerType

	/**
	 * Registers a [plugin] for the uninstall session.
	 * @param plugin Kotlin class of a registered plugin, implementing [AckpineUninstallPlugin].
	 * @param parameters parameters of the registered plugin for the session being configured.
	 */
	public fun <Params : AckpinePlugin.Parameters> plugin(
		plugin: KClass<out AckpineUninstallPlugin<Params>>,
		parameters: Params
	)

	/**
	 * Registers a [plugin] for the uninstall session.
	 * @param plugin Kotlin class of a registered plugin, implementing [AckpineUninstallPlugin].
	 */
	public fun plugin(plugin: KClass<out AckpineUninstallPlugin<AckpinePlugin.Parameters.None>>)
}

@PublishedApi
internal class UninstallParametersDslBuilder(packageName: String) : UninstallParametersDsl {

	private val builder = UninstallParameters.Builder(packageName)

	override var confirmation: Confirmation
		get() = builder.confirmation
		set(value) {
			builder.setConfirmation(value)
		}

	override var notificationData: NotificationData
		get() = builder.notificationData
		set(value) {
			builder.setNotificationData(value)
		}

	override var packageName: String
		get() = builder.packageName
		set(value) {
			builder.setPackageName(value)
		}

	override var uninstallerType: UninstallerType
		get() = builder.uninstallerType
		set(value) {
			builder.setUninstallerType(value)
		}

	override var packageVersionCode: PackageVersionCode
		get() = builder.packageVersionCode
		set(value) {
			builder.setPackageVersionCode(value)
		}

	override val versionedUninstallPolicy: VersionedUninstallPolicy
		get() = builder.versionedUninstallPolicy

	override fun <Params : AckpinePlugin.Parameters> plugin(
		plugin: KClass<out AckpineUninstallPlugin<Params>>,
		parameters: Params
	) {
		builder.registerPlugin(plugin.java, parameters)
	}

	override fun packageVersionCode(packageVersionCode: PackageVersionCode, policy: VersionedUninstallPolicy) {
		builder.setPackageVersionCode(packageVersionCode, policy)
	}

	override fun plugin(plugin: KClass<out AckpineUninstallPlugin<AckpinePlugin.Parameters.None>>) {
		builder.registerPlugin(plugin.java)
	}

	fun build() = builder.build()
}

/**
 * Registers a plugin for the uninstall session. [Plugin] is the type of the plugin being registered.
 */
public inline fun <reified Plugin : AckpineUninstallPlugin<AckpinePlugin.Parameters.None>> UninstallParametersDsl.plugin() {
	plugin(Plugin::class)
}