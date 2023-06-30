package ru.solrudev.ackpine.impl.installer.session

import android.content.Context
import android.net.Uri
import android.os.Handler
import ru.solrudev.ackpine.impl.database.dao.SessionDao
import ru.solrudev.ackpine.impl.database.dao.SessionFailureDao
import ru.solrudev.ackpine.impl.database.dao.SessionProgressDao
import ru.solrudev.ackpine.impl.session.AbstractProgressSession
import ru.solrudev.ackpine.installer.InstallFailure
import ru.solrudev.ackpine.session.Progress
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.session.parameters.Confirmation
import ru.solrudev.ackpine.session.parameters.NotificationData
import java.util.UUID
import java.util.concurrent.Executor

internal class SessionBasedInstallSession internal constructor(
	private val context: Context,
	private val apks: List<Uri>,
	id: UUID,
	initialState: Session.State<InstallFailure>,
	initialProgress: Progress,
	private val confirmation: Confirmation,
	private val notificationData: NotificationData,
	sessionDao: SessionDao,
	sessionFailureDao: SessionFailureDao<InstallFailure>,
	sessionProgressDao: SessionProgressDao,
	executor: Executor,
	handler: Handler,
) : AbstractProgressSession<InstallFailure>(
	id, initialState, initialProgress, sessionDao, sessionFailureDao, sessionProgressDao, executor, handler,
	InstallFailure::Exceptional
) {

	override fun doLaunch() {
		TODO("Not yet implemented")
	}

	override fun doCommit() {
		TODO("Not yet implemented")
	}

	override fun doCancel() {
		TODO("Not yet implemented")
	}

	override fun cleanup() {
		TODO("Not yet implemented")
	}
}