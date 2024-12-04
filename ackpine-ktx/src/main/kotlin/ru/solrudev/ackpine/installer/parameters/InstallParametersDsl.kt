/*
 * Copyright (C) 2023-2024 Ilya Fomichev
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
import ru.solrudev.ackpine.session.parameters.Confirmation
import ru.solrudev.ackpine.session.parameters.ConfirmationDsl
import ru.solrudev.ackpine.session.parameters.NotificationData
import ru.solrudev.ackpine.session.parameters.SessionParametersDsl
import java.util.Locale
import kotlin.time.Duration

/**
 * DSL allowing to configure [parameters for creating install session][InstallParameters].
 */
@SessionParametersDsl
public interface InstallParametersDsl : ConfirmationDsl {

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
	 * Applying this option is best-effort. It takes effect only on API level >= 31 with [InstallerType.SESSION_BASED]
	 * installer type.
	 *
	 * @see [PackageInstaller.SessionParams.setRequireUserAction]
	 */
	public var requireUserAction: Boolean

	/**
	 * Mode for an install session. Takes effect only when using [InstallerType.SESSION_BASED] installer.
	 *
	 * Default value is [InstallMode.Full].
	 */
	public var installMode: InstallMode

	public var preapproval: InstallPreapproval

	public var constraints: InstallConstraints

	public var requestUpdateOwnership: Boolean

	public var packageSource: PackageSource

	public fun constraints(timeout: Duration, configure: InstallConstraintsDsl.() -> Unit)

	public fun preapproval(
		packageName: String,
		label: String,
		languageTag: String
	)

	@RequiresApi(Build.VERSION_CODES.N)
	public fun preapproval(
		packageName: String,
		label: String,
		locale: ULocale
	)

	@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
	public fun preapproval(
		packageName: String,
		label: String,
		locale: Locale
	)

	public fun preapproval(
		packageName: String,
		label: String,
		languageTag: String,
		configure: InstallPreapprovalDsl.() -> Unit
	)

	@RequiresApi(Build.VERSION_CODES.N)
	public fun preapproval(
		packageName: String,
		label: String,
		locale: ULocale,
		configure: InstallPreapprovalDsl.() -> Unit
	)

	@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
	public fun preapproval(
		packageName: String,
		label: String,
		locale: Locale,
		configure: InstallPreapprovalDsl.() -> Unit
	)
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

	override fun constraints(timeout: Duration, configure: InstallConstraintsDsl.() -> Unit) {
		constraints = InstallConstraints(timeout, configure)
	}

	override fun preapproval(packageName: String, label: String, languageTag: String) {
		preapproval = InstallPreapproval(packageName, label, languageTag)
	}

	@RequiresApi(Build.VERSION_CODES.N)
	override fun preapproval(packageName: String, label: String, locale: ULocale) {
		preapproval = InstallPreapproval(packageName, label, locale)
	}

	@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
	override fun preapproval(packageName: String, label: String, locale: Locale) {
		preapproval = InstallPreapproval(packageName, label, locale)
	}

	override fun preapproval(
		packageName: String,
		label: String,
		languageTag: String,
		configure: InstallPreapprovalDsl.() -> Unit
	) {
		preapproval = InstallPreapproval(packageName, label, languageTag, configure)
	}

	@RequiresApi(Build.VERSION_CODES.N)
	override fun preapproval(
		packageName: String,
		label: String,
		locale: ULocale,
		configure: InstallPreapprovalDsl.() -> Unit
	) {
		preapproval = InstallPreapproval(packageName, label, locale, configure)
	}

	@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
	override fun preapproval(
		packageName: String,
		label: String,
		locale: Locale,
		configure: InstallPreapprovalDsl.() -> Unit
	) {
		preapproval = InstallPreapproval(packageName, label, locale, configure)
	}

	fun build() = builder.build()
}