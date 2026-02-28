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

package ru.solrudev.ackpine.remote.dsl

import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import ru.solrudev.ackpine.DelicateAckpineApi
import ru.solrudev.ackpine.installer.parameters.InstallParameters
import ru.solrudev.ackpine.installer.parameters.InstallPreapproval
import ru.solrudev.ackpine.installer.parameters.InstallerType
import ru.solrudev.ackpine.remote.RemoteNotificationData
import ru.solrudev.ackpine.session.parameters.Confirmation
import ru.solrudev.ackpine.session.parameters.SessionParametersDsl
import java.util.Locale

/**
 * DSL allowing to configure parameters for creating install session.
 */
@SessionParametersDsl
public interface RemoteInstallParametersDsl : RemoteConfirmationDsl {

	/**
	 * Type of the package installer implementation.
	 *
	 * Default value is [InstallerType.DEFAULT].
	 *
	 * When getting/setting the value of this property, the following invariants are maintained:
	 * * When on API level < 21, [InstallerType.INTENT_BASED] is always returned/set regardless of the
	 * current/provided value;
	 * * When on API level >= 21 and `apks` contain more than one entry, [InstallerType.SESSION_BASED] is always
	 * returned/set regardless of the current/provided value.
	 */
	public var installerType: InstallerType

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
}

internal class RemoteInstallParameters(
	val installParameters: InstallParameters,
	val notificationData: RemoteNotificationData
) {
	internal class Builder(val parametersBuilder: InstallParameters.Builder) {
		fun build(notificationData: RemoteNotificationData) = RemoteInstallParameters(
			parametersBuilder.build(),
			notificationData
		)
	}
}

@PublishedApi
internal class RemoteInstallParametersDslBuilder : RemoteInstallParametersDsl {

	constructor(baseApk: Uri) {
		builder = RemoteInstallParameters.Builder(InstallParameters.Builder(baseApk))
	}

	@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
	constructor(apks: Iterable<Uri>) {
		builder = RemoteInstallParameters.Builder(InstallParameters.Builder(apks))
	}

	private val builder: RemoteInstallParameters.Builder

	override var installerType: InstallerType
		get() = builder.parametersBuilder.installerType
		set(value) {
			builder.parametersBuilder.setInstallerType(value)
		}

	override var confirmation: Confirmation
		get() = builder.parametersBuilder.confirmation
		set(value) {
			builder.parametersBuilder.setConfirmation(value)
		}

	override var notificationData = RemoteNotificationData.DEFAULT

	@set:DelicateAckpineApi
	override var requireUserAction: Boolean
		get() = builder.parametersBuilder.requireUserAction
		set(value) {
			builder.parametersBuilder.setRequireUserAction(value)
		}

	override var preapproval: InstallPreapproval
		get() = builder.parametersBuilder.preapproval
		set(value) {
			builder.parametersBuilder.setPreapproval(value)
		}

	fun build() = builder.build(notificationData)
}

/**
 * Configures pre-commit install approval.
 * @param packageName the package name of the app to be installed.
 * @param label the label representing the app to be installed.
 * @param locale the locale of the app label being used.
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public fun RemoteInstallParametersDsl.preapproval(
	packageName: String,
	label: String,
	locale: Locale,
	icon: Uri = Uri.EMPTY,
	fallbackToOnDemandApproval: Boolean = false
) {
	preapproval = InstallPreapproval(packageName, label, locale) {
		this.icon = icon
		this.fallbackToOnDemandApproval = fallbackToOnDemandApproval
	}
}