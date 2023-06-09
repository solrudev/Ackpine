package ru.solrudev.ackpine.impl.installer

import android.annotation.SuppressLint
import androidx.annotation.RestrictTo
import androidx.concurrent.futures.ResolvableFuture
import androidx.core.net.toUri
import com.google.common.util.concurrent.ListenableFuture
import ru.solrudev.ackpine.helpers.safeExecuteWith
import ru.solrudev.ackpine.impl.database.dao.InstallSessionDao
import ru.solrudev.ackpine.impl.database.dao.SessionProgressDao
import ru.solrudev.ackpine.impl.database.model.SessionEntity
import ru.solrudev.ackpine.impl.session.toSessionState
import ru.solrudev.ackpine.installer.InstallFailure
import ru.solrudev.ackpine.installer.PackageInstaller
import ru.solrudev.ackpine.installer.parameters.InstallParameters
import ru.solrudev.ackpine.session.Progress
import ru.solrudev.ackpine.session.ProgressSession
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.session.parameters.NotificationData
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class PackageInstallerImpl internal constructor(
	private val installSessionDao: InstallSessionDao,
	private val sessionProgressDao: SessionProgressDao,
	private val executor: Executor,
	private val installSessionFactory: InstallSessionFactory
) : PackageInstaller {

	private val sessions = ConcurrentHashMap<UUID, ProgressSession<InstallFailure>>()

	@Volatile
	private var isSessionsMapInitialized = false

	override fun createSession(parameters: InstallParameters): ProgressSession<InstallFailure> {
		val id = UUID.randomUUID()
		executor.execute {
			installSessionDao.insertInstallSession(
				SessionEntity.InstallSession(
					session = SessionEntity(
						id.toString(),
						SessionEntity.State.PENDING,
						parameters.confirmation,
						parameters.notificationData.title,
						parameters.notificationData.contentText,
						parameters.notificationData.icon
					),
					installerType = parameters.installerType,
					uris = parameters.apks.toList().map { it.toString() }
				)
			)
		}
		val session = installSessionFactory.create(
			parameters, id,
			initialState = Session.State.Pending,
			initialProgress = Progress()
		)
		sessions[id] = session
		return session
	}

	@SuppressLint("RestrictedApi")
	override fun getSessionAsync(sessionId: UUID): ListenableFuture<ProgressSession<InstallFailure>?> {
		val future = ResolvableFuture.create<ProgressSession<InstallFailure>?>()
		sessions[sessionId]?.let(future::set) ?: executor.safeExecuteWith(future) {
			val session = installSessionDao.getInstallSession(sessionId.toString())
			val installSession = session?.toInstallSession()?.let { sessions.putIfAbsent(sessionId, it) ?: it }
			future.set(installSession)
		}
		return future
	}

	@SuppressLint("RestrictedApi")
	override fun getSessionsAsync(): ListenableFuture<List<ProgressSession<InstallFailure>>> {
		return if (isSessionsMapInitialized) {
			ResolvableFuture.create<List<ProgressSession<InstallFailure>>>().apply {
				set(sessions.values.toList())
			}
		} else {
			initializeSessions { sessions -> sessions.toList() }
		}
	}

	@SuppressLint("RestrictedApi")
	override fun getActiveSessionsAsync(): ListenableFuture<List<ProgressSession<InstallFailure>>> {
		return if (isSessionsMapInitialized) {
			ResolvableFuture.create<List<ProgressSession<InstallFailure>>>().apply {
				set(sessions.values.filter { it.isActive })
			}
		} else {
			initializeSessions { sessions -> sessions.filter { it.isActive } }
		}
	}

	@SuppressLint("RestrictedApi")
	private inline fun initializeSessions(
		crossinline transform: (Iterable<ProgressSession<InstallFailure>>) -> List<ProgressSession<InstallFailure>>
	): ListenableFuture<List<ProgressSession<InstallFailure>>> {
		val future = ResolvableFuture.create<List<ProgressSession<InstallFailure>>>()
		executor.safeExecuteWith(future) {
			installSessionDao.getInstallSessions().forEach { session ->
				val installSession = session.toInstallSession()
				sessions.putIfAbsent(installSession.id, installSession)
			}
			isSessionsMapInitialized = true
			future.set(transform(sessions.values))
		}
		return future
	}

	@SuppressLint("NewApi")
	private fun SessionEntity.InstallSession.toInstallSession(): ProgressSession<InstallFailure> {
		val parameters = InstallParameters.Builder(uris.map(String::toUri))
			.setInstallerType(installerType)
			.setConfirmation(session.confirmation)
			.setNotificationData(
				NotificationData.Builder()
					.setTitle(session.notificationTitle)
					.setContentText(session.notificationText)
					.setIcon(session.notificationIcon)
					.build()
			)
			.build()
		return installSessionFactory.create(
			parameters,
			UUID.fromString(session.id),
			initialState = session.state.toSessionState(session.id, installSessionDao),
			initialProgress = sessionProgressDao.getProgress(session.id) ?: Progress()
		)
	}
}