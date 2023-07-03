package ru.solrudev.ackpine.impl.uninstaller.session

import android.content.Context
import android.os.Handler
import androidx.annotation.RestrictTo
import ru.solrudev.ackpine.impl.database.dao.SessionDao
import ru.solrudev.ackpine.impl.database.dao.SessionFailureDao
import ru.solrudev.ackpine.impl.session.AbstractSession
import ru.solrudev.ackpine.impl.session.helpers.UPDATE_CURRENT_FLAGS
import ru.solrudev.ackpine.impl.session.helpers.launchConfirmation
import ru.solrudev.ackpine.impl.uninstaller.activity.UninstallActivity
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.session.parameters.Confirmation
import ru.solrudev.ackpine.session.parameters.NotificationData
import ru.solrudev.ackpine.uninstaller.UninstallFailure
import java.util.UUID
import java.util.concurrent.Executor

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class UninstallSession internal constructor(
	private val context: Context,
	private val packageName: String,
	id: UUID,
	initialState: Session.State<UninstallFailure>,
	private val confirmation: Confirmation,
	private val notificationData: NotificationData,
	sessionDao: SessionDao,
	sessionFailureDao: SessionFailureDao<UninstallFailure>,
	executor: Executor,
	handler: Handler
) : AbstractSession<UninstallFailure>(
	id, initialState, sessionDao, sessionFailureDao, executor, handler,
	exceptionalFailureFactory = UninstallFailure::Exceptional
) {

	override fun doLaunch() {}

	override fun doCommit() {
		context.launchConfirmation<UninstallActivity>(
			confirmation,
			notificationData,
			id,
			UNINSTALLER_NOTIFICATION_TAG,
			UNINSTALLER_REQUEST_CODE,
			UPDATE_CURRENT_FLAGS
		) { intent -> intent.putExtra(UninstallActivity.PACKAGE_NAME_KEY, packageName) }
	}

	override fun doCancel() {}
}