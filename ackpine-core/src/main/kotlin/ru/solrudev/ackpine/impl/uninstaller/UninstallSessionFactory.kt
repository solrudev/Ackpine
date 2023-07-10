package ru.solrudev.ackpine.impl.uninstaller

import android.content.Context
import android.os.Handler
import androidx.annotation.RestrictTo
import ru.solrudev.ackpine.R
import ru.solrudev.ackpine.helpers.SerialExecutor
import ru.solrudev.ackpine.impl.database.dao.NotificationIdDao
import ru.solrudev.ackpine.impl.database.dao.SessionDao
import ru.solrudev.ackpine.impl.database.dao.SessionFailureDao
import ru.solrudev.ackpine.impl.uninstaller.helpers.getApplicationLabel
import ru.solrudev.ackpine.impl.uninstaller.session.UninstallSession
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.session.parameters.NotificationData
import ru.solrudev.ackpine.session.parameters.NotificationString
import ru.solrudev.ackpine.uninstaller.UninstallFailure
import ru.solrudev.ackpine.uninstaller.parameters.UninstallParameters
import java.util.UUID
import java.util.concurrent.Executor

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal interface UninstallSessionFactory {

	fun create(
		parameters: UninstallParameters,
		id: UUID,
		initialState: Session.State<UninstallFailure>
	): Session<UninstallFailure>
}

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class UninstallSessionFactoryImpl internal constructor(
	private val applicationContext: Context,
	private val sessionDao: SessionDao,
	private val sessionFailureDao: SessionFailureDao<UninstallFailure>,
	private val notificationIdDao: NotificationIdDao,
	private val executor: Executor,
	private val handler: Handler
) : UninstallSessionFactory {

	override fun create(
		parameters: UninstallParameters,
		id: UUID,
		initialState: Session.State<UninstallFailure>
	): Session<UninstallFailure> {
		return UninstallSession(
			applicationContext,
			parameters.packageName,
			id, initialState,
			parameters.confirmation,
			parameters.notificationData.resolveDefault(parameters.packageName),
			sessionDao, sessionFailureDao, notificationIdDao,
			SerialExecutor(executor), handler
		)
	}

	private fun NotificationData.resolveDefault(packageName: String): NotificationData = NotificationData.Builder()
		.setTitle(
			title.takeUnless { it.isDefault } ?: NotificationString.resource(R.string.ackpine_prompt_uninstall_title)
		)
		.setContentText(
			contentText.takeUnless { it.isDefault } ?: resolveDefaultContentText(packageName)
		)
		.setIcon(icon)
		.build()

	private fun resolveDefaultContentText(packageName: String): NotificationString {
		val label = applicationContext.packageManager.getApplicationLabel(packageName)?.toString()
		return if (label != null) {
			NotificationString.resource(R.string.ackpine_prompt_uninstall_message_with_label, label)
		} else {
			NotificationString.resource(R.string.ackpine_prompt_uninstall_message)
		}
	}
}