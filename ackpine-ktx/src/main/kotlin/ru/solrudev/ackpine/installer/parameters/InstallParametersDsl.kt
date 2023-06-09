package ru.solrudev.ackpine.installer.parameters

import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import ru.solrudev.ackpine.session.parameters.Confirmation
import ru.solrudev.ackpine.session.parameters.ConfirmationDsl
import ru.solrudev.ackpine.session.parameters.NotificationData
import ru.solrudev.ackpine.session.parameters.SessionParametersDsl

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
	 * When getting/setting the value of this property, the following invariants are taken into account:
	 * * When on API level < 21, [InstallerType.INTENT_BASED] is always returned/set regardless of the
	 * current/provided value;
	 * * When on API level >= 21 and [apks] contain more than one entry, [InstallerType.SESSION_BASED] is always
	 * returned/set regardless of the current/provided value.
	 */
	public var installerType: InstallerType
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
		get() = builder.getApks() as MutableApkList

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

	fun build() = builder.build()
}