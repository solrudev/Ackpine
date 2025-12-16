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

package ru.solrudev.ackpine.installer.parameters

import android.content.pm.PackageInstaller
import android.icu.util.ULocale
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import ru.solrudev.ackpine.DelicateAckpineApi
import ru.solrudev.ackpine.plugability.AckpinePlugin
import ru.solrudev.ackpine.plugability.AckpinePlugin.Parameters
import ru.solrudev.ackpine.plugability.AckpinePluginRegistryDsl
import ru.solrudev.ackpine.session.parameters.Confirmation
import ru.solrudev.ackpine.session.parameters.ConfirmationDsl
import ru.solrudev.ackpine.session.parameters.NotificationData
import ru.solrudev.ackpine.session.parameters.SessionParametersDsl
import java.util.Locale
import kotlin.reflect.KClass
import kotlin.time.Duration

/**
 * DSL allowing to configure [parameters for creating install session][InstallParameters].
 */
@SessionParametersDsl
public interface InstallParametersDsl : ConfirmationDsl, AckpinePluginRegistryDsl {

	/**
	 * Mutable list of APKs [URIs][Uri] to install in one session.
	 */
	public val apks: MutableApkList

	/**
	 * Type of the package installer implementation.
	 *
	 * Default value is [InstallerType.DEFAULT].
	 *
	 * When getting/setting the value of this property, the following invariants are maintained:
	 * * When on API level < 21, [InstallerType.INTENT_BASED] is always returned/set regardless of the
	 * current/provided value;
	 * * When on API level >= 21 and [apks] contain more than one entry, [InstallerType.SESSION_BASED] is always
	 * returned/set regardless of the current/provided value.
	 */
	public var installerType: InstallerType

	/**
	 * Optional name of the session. It may be a name of the app being installed or a file name. Used in default
	 * notification content text.
	 */
	public var name: String

	/**
	 * Indicate whether user action should be required when the session is committed. By default equals to `true`.
	 *
	 * Applying this option is best-effort. It takes effect only on API level >= [31][Build.VERSION_CODES.S] with
	 * [InstallerType.SESSION_BASED] installer type.
	 *
	 * This is a **delicate** API. This option is unstable for use on different Android versions from different vendors.
	 * It's recommended to avoid using it on API level < 33 and on devices with modified OS package installer, most
	 * notably from Chinese vendors, unless your app is privileged for silent installs.
	 *
	 * @see [PackageInstaller.SessionParams.setRequireUserAction]
	 */
	@set:DelicateAckpineApi
	public var requireUserAction: Boolean

	/**
	 * Mode for an install session. Takes effect only when using [InstallerType.SESSION_BASED] installer.
	 *
	 * Default value is [InstallMode.Full].
	 */
	public var installMode: InstallMode

	/**
	 * Details for requesting the pre-commit install approval.
	 *
	 * Applying this option is best-effort. It takes effect only on API level >=
	 * [34][Build.VERSION_CODES.UPSIDE_DOWN_CAKE] with [InstallerType.SESSION_BASED] installer type.
	 *
	 * Default value is [InstallPreapproval.NONE].
	 *
	 * @see [PackageInstaller.Session.requestUserPreapproval]
	 */
	public var preapproval: InstallPreapproval

	/**
	 * Installation constraints.
	 *
	 * Applying this option is best-effort. It takes effect only on API level >=
	 * [34][Build.VERSION_CODES.UPSIDE_DOWN_CAKE] with [InstallerType.SESSION_BASED] installer type.
	 *
	 * Default value is [InstallConstraints.NONE].
	 *
	 * @see [PackageInstaller.InstallConstraints]
	 */
	public var constraints: InstallConstraints

	/**
	 * Optionally indicate whether the package being installed needs the update ownership
	 * enforcement. Once the update ownership enforcement is enabled, the other installers
	 * will need the user action to update the package even if the installers have been
	 * granted the `INSTALL_PACKAGES` permission. Default to `false`.
	 *
	 * The update ownership enforcement can only be enabled on initial installation. Setting
	 * this to `true` on package update is a no-op.
	 *
	 * Applying this option is best-effort. It takes effect only on API level >=
	 * [34][Build.VERSION_CODES.UPSIDE_DOWN_CAKE] with [InstallerType.SESSION_BASED] installer type.
	 */
	public var requestUpdateOwnership: Boolean

	/**
	 * Indicates the package source of the app being installed. This is informational and may be used as a signal
	 * by the system.
	 *
	 * Default value is [PackageSource.Unspecified].
	 */
	public var packageSource: PackageSource
}

@PublishedApi
internal class InstallParametersDslBuilder : InstallParametersDsl {

	constructor(baseApk: Uri) {
		builder = InstallParameters.Builder(baseApk)
	}

	@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
	constructor(apks: Iterable<Uri>) {
		builder = InstallParameters.Builder(apks)
	}

	private val builder: InstallParameters.Builder

	override val apks: MutableApkList
		get() = builder.apks as MutableApkList

	override var installerType: InstallerType
		get() = builder.installerType
		set(value) {
			builder.setInstallerType(value)
		}

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

	override var name: String
		get() = builder.name
		set(value) {
			builder.setName(value)
		}

	@set:DelicateAckpineApi
	override var requireUserAction: Boolean
		get() = builder.requireUserAction
		set(value) {
			builder.setRequireUserAction(value)
		}

	override var installMode: InstallMode
		get() = builder.installMode
		set(value) {
			builder.setInstallMode(value)
		}

	override var preapproval: InstallPreapproval
		get() = builder.preapproval
		set(value) {
			builder.setPreapproval(value)
		}

	override var constraints: InstallConstraints
		get() = builder.constraints
		set(value) {
			builder.setConstraints(value)
		}

	override var requestUpdateOwnership: Boolean
		get() = builder.requestUpdateOwnership
		set(value) {
			builder.setRequestUpdateOwnership(value)
		}

	override var packageSource: PackageSource
		get() = builder.packageSource
		set(value) {
			builder.setPackageSource(value)
		}

	override fun <Params : Parameters> usePlugin(
		plugin: KClass<out AckpinePlugin<Params>>,
		parameters: Params
	) {
		builder.usePlugin(plugin.java, parameters)
	}

	override fun usePlugin(plugin: KClass<out AckpinePlugin<Parameters.None>>) {
		builder.usePlugin(plugin.java)
	}

	fun build() = builder.build()
}

/**
 * Configures [installation constraints DSL][InstallConstraintsDsl].
 * @param timeout the maximum time to wait, in milliseconds until the constraints are satisfied.
 */
public inline fun InstallParametersDsl.constraints(
	timeout: Duration,
	configure: InstallConstraintsDsl.() -> Unit
) {
	constraints = InstallConstraints(timeout, configure)
}

/**
 * Configures [pre-commit install approval DSL][InstallPreapprovalDsl].
 * @param packageName the package name of the app to be installed.
 * @param label the label representing the app to be installed.
 * @param languageTag the locale of the app label being used. Represented by IETF BCP 47 language tag.
 */
public inline fun InstallParametersDsl.preapproval(
	packageName: String,
	label: String,
	languageTag: String,
	configure: InstallPreapprovalDsl.() -> Unit = {}
) {
	preapproval = InstallPreapproval(packageName, label, languageTag, configure)
}

/**
 * Configures [pre-commit install approval DSL][InstallPreapprovalDsl].
 * @param packageName the package name of the app to be installed.
 * @param label the label representing the app to be installed.
 * @param locale the locale of the app label being used.
 */
@RequiresApi(Build.VERSION_CODES.N)
public inline fun InstallParametersDsl.preapproval(
	packageName: String,
	label: String,
	locale: ULocale,
	configure: InstallPreapprovalDsl.() -> Unit = {}
) {
	preapproval = InstallPreapproval(packageName, label, locale, configure)
}

/**
 * Configures [pre-commit install approval DSL][InstallPreapprovalDsl].
 * @param packageName the package name of the app to be installed.
 * @param label the label representing the app to be installed.
 * @param locale the locale of the app label being used.
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public inline fun InstallParametersDsl.preapproval(
	packageName: String,
	label: String,
	locale: Locale,
	configure: InstallPreapprovalDsl.() -> Unit = {}
) {
	preapproval = InstallPreapproval(packageName, label, locale, configure)
}

/**
 * Configures [pre-commit install approval][InstallPreapproval].
 * @param packageName the package name of the app to be installed.
 * @param label the label representing the app to be installed.
 * @param languageTag the locale of the app label being used. Represented by IETF BCP 47 language tag.
 * @param icon the icon representing the app to be installed.
 */
public fun InstallParametersDsl.preapproval(
	packageName: String,
	label: String,
	languageTag: String,
	icon: Uri
) {
	preapproval = InstallPreapproval(packageName, label, languageTag, icon)
}

/**
 * Configures [pre-commit install approval][InstallPreapproval].
 * @param packageName the package name of the app to be installed.
 * @param label the label representing the app to be installed.
 * @param locale the locale of the app label being used.
 * @param icon the icon representing the app to be installed.
 */
@RequiresApi(Build.VERSION_CODES.N)
public fun InstallParametersDsl.preapproval(
	packageName: String,
	label: String,
	locale: ULocale,
	icon: Uri
) {
	preapproval = InstallPreapproval(packageName, label, locale, icon)
}

/**
 * Configures [pre-commit install approval][InstallPreapproval].
 * @param packageName the package name of the app to be installed.
 * @param label the label representing the app to be installed.
 * @param locale the locale of the app label being used.
 * @param icon the icon representing the app to be installed.
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public fun InstallParametersDsl.preapproval(
	packageName: String,
	label: String,
	locale: Locale,
	icon: Uri
) {
	preapproval = InstallPreapproval(packageName, label, locale, icon)
}