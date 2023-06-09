package ru.solrudev.ackpine.uninstaller.parameters

import ru.solrudev.ackpine.session.parameters.Confirmation
import ru.solrudev.ackpine.session.parameters.ConfirmationDsl
import ru.solrudev.ackpine.session.parameters.NotificationData
import ru.solrudev.ackpine.session.parameters.SessionParametersDsl

/**
 * DSL allowing to configure [parameters for creating uninstall session][UninstallParameters].
 */
@SessionParametersDsl
public interface UninstallParametersDsl : ConfirmationDsl {

	/**
	 * Name of the package to be uninstalled.
	 */
	public var packageName: String
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

	fun build() = builder.build()
}