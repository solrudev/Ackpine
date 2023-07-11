package ru.solrudev.ackpine.impl.uninstaller.session

import android.content.Context
import android.os.CancellationSignal
import android.os.Handler
import androidx.annotation.RestrictTo
import ru.solrudev.ackpine.impl.database.dao.NotificationIdDao
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
	notificationIdDao: NotificationIdDao,
	serialExecutor: Executor,
	handler: Handler
) : AbstractSession<UninstallFailure>(
	context, UNINSTALLER_NOTIFICATION_TAG,
	id, initialState,
	sessionDao, sessionFailureDao, notificationIdDao,
	serialExecutor, handler,
	exceptionalFailureFactory = UninstallFailure::Exceptional
) {

	override fun prepare(cancellationSignal: CancellationSignal) {
		// no preparation needed
		notifyAwaiting()
	}

	override fun launchConfirmation(cancellationSignal: CancellationSignal, notificationId: Int) {
		context.launchConfirmation<UninstallActivity>(
			confirmation, notificationData,
			sessionId = id,
			UNINSTALLER_NOTIFICATION_TAG, notificationId,
			UNINSTALLER_REQUEST_CODE,
			UPDATE_CURRENT_FLAGS
		) { intent -> intent.putExtra(UninstallActivity.PACKAGE_NAME_KEY, packageName) }
	}
}