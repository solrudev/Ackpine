package ru.solrudev.ackpine.impl.uninstaller

import android.annotation.SuppressLint
import androidx.annotation.RestrictTo
import androidx.concurrent.futures.ResolvableFuture
import com.google.common.util.concurrent.ListenableFuture
import ru.solrudev.ackpine.helpers.safeExecuteWith
import ru.solrudev.ackpine.impl.database.dao.UninstallSessionDao
import ru.solrudev.ackpine.impl.database.model.SessionEntity
import ru.solrudev.ackpine.impl.session.toSessionState
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.session.parameters.NotificationData
import ru.solrudev.ackpine.uninstaller.PackageUninstaller
import ru.solrudev.ackpine.uninstaller.UninstallFailure
import ru.solrudev.ackpine.uninstaller.parameters.UninstallParameters
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class PackageUninstallerImpl internal constructor(
	private val uninstallSessionDao: UninstallSessionDao,
	private val executor: Executor,
	private val uninstallSessionFactory: UninstallSessionFactory
) : PackageUninstaller {

	private val sessions = ConcurrentHashMap<UUID, Session<UninstallFailure>>()

	@Volatile
	private var isSessionsMapInitialized = false

	override fun createSession(parameters: UninstallParameters): Session<UninstallFailure> {
		val id = UUID.randomUUID()
		executor.execute {
			uninstallSessionDao.insertUninstallSession(
				SessionEntity.UninstallSession(
					session = SessionEntity(
						id.toString(),
						SessionEntity.State.PENDING,
						parameters.confirmation,
						parameters.notificationData.title,
						parameters.notificationData.contentText,
						parameters.notificationData.icon
					),
					packageName = parameters.packageName
				)
			)
		}
		val session = uninstallSessionFactory.create(
			parameters, id,
			initialState = Session.State.Pending
		)
		sessions[id] = session
		return session
	}

	@SuppressLint("RestrictedApi")
	override fun getSessionAsync(sessionId: UUID): ListenableFuture<Session<UninstallFailure>?> {
		val future = ResolvableFuture.create<Session<UninstallFailure>?>()
		sessions[sessionId]?.let(future::set) ?: executor.safeExecuteWith(future) {
			val session = uninstallSessionDao.getUninstallSession(sessionId.toString())
			val uninstallSession = session?.toUninstallSession()?.let {
				sessions.putIfAbsent(sessionId, it) ?: it
			}
			future.set(uninstallSession)
		}
		return future
	}

	@SuppressLint("RestrictedApi")
	override fun getSessionsAsync(): ListenableFuture<List<Session<UninstallFailure>>> {
		return if (isSessionsMapInitialized) {
			ResolvableFuture.create<List<Session<UninstallFailure>>>().apply {
				set(sessions.values.toList())
			}
		} else {
			initializeSessions { sessions -> sessions.toList() }
		}
	}

	@SuppressLint("RestrictedApi")
	override fun getActiveSessionsAsync(): ListenableFuture<List<Session<UninstallFailure>>> {
		return if (isSessionsMapInitialized) {
			ResolvableFuture.create<List<Session<UninstallFailure>>>().apply {
				set(sessions.values.filter { it.isActive })
			}
		} else {
			initializeSessions { sessions -> sessions.filter { it.isActive } }
		}
	}

	@SuppressLint("RestrictedApi")
	private inline fun initializeSessions(
		crossinline transform: (Iterable<Session<UninstallFailure>>) -> List<Session<UninstallFailure>>
	): ListenableFuture<List<Session<UninstallFailure>>> {
		val future = ResolvableFuture.create<List<Session<UninstallFailure>>>()
		executor.safeExecuteWith(future) {
			uninstallSessionDao.getUninstallSessions().forEach { session ->
				val uninstallSession = session.toUninstallSession()
				sessions.putIfAbsent(uninstallSession.id, uninstallSession)
			}
			isSessionsMapInitialized = true
			future.set(transform(sessions.values))
		}
		return future
	}

	private fun SessionEntity.UninstallSession.toUninstallSession(): Session<UninstallFailure> {
		val parameters = UninstallParameters.Builder(packageName)
			.setConfirmation(session.confirmation)
			.setNotificationData(
				NotificationData.Builder()
					.setTitle(session.notificationTitle)
					.setContentText(session.notificationText)
					.setIcon(session.notificationIcon)
					.build()
			)
			.build()
		return uninstallSessionFactory.create(
			parameters,
			UUID.fromString(session.id),
			initialState = session.state.toSessionState(session.id, uninstallSessionDao)
		)
	}
}