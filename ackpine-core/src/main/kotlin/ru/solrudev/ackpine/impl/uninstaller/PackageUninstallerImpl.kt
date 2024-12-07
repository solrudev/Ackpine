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

package ru.solrudev.ackpine.impl.uninstaller

import androidx.annotation.RestrictTo
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.concurrent.futures.CallbackToFutureAdapter.Completer
import com.google.common.util.concurrent.ListenableFuture
import ru.solrudev.ackpine.helpers.concurrent.BinarySemaphore
import ru.solrudev.ackpine.helpers.concurrent.executeWithCompleter
import ru.solrudev.ackpine.helpers.concurrent.executeWithSemaphore
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

private typealias SessionsCollectionTransformer =
			(Collection<Session<UninstallFailure>>) -> List<Session<UninstallFailure>>

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class PackageUninstallerImpl internal constructor(
	private val uninstallSessionDao: UninstallSessionDao,
	private val executor: Executor,
	private val uninstallSessionFactory: UninstallSessionFactory,
	private val uuidFactory: () -> UUID,
	private val notificationIdFactory: () -> Int
) : PackageUninstaller {

	private val sessions = ConcurrentHashMap<UUID, Session<UninstallFailure>>()

	@Volatile
	private var isSessionsMapInitialized = false

	override fun createSession(parameters: UninstallParameters): Session<UninstallFailure> {
		val id = uuidFactory()
		val notificationId = notificationIdFactory()
		val dbWriteSemaphore = BinarySemaphore()
		val session = uninstallSessionFactory.create(
			parameters, id,
			initialState = Session.State.Pending,
			notificationId, dbWriteSemaphore
		)
		sessions[id] = session
		persistSession(id, parameters, dbWriteSemaphore, notificationId)
		return session
	}

	override fun getSessionAsync(sessionId: UUID) = CallbackToFutureAdapter.getFuture { completer ->
		sessions[sessionId]?.let(completer::set) ?: executor.executeWithCompleter(completer) {
			getSessionFromDb(sessionId, completer)
		}
		"PackageUninstallerImpl.getSessionAsync($sessionId)"
	}

	override fun getSessionsAsync(): ListenableFuture<List<Session<UninstallFailure>>> {
		val tag = "PackageUninstallerImpl.getSessionsAsync"
		if (isSessionsMapInitialized) {
			return CallbackToFutureAdapter.getFuture { completer ->
				completer.set(sessions.values.toList())
				tag
			}
		}
		return initializeSessions(tag) { sessions -> sessions.toList() }
	}

	override fun getActiveSessionsAsync(): ListenableFuture<List<Session<UninstallFailure>>> {
		val tag = "PackageUninstallerImpl.getActiveSessionsAsync"
		if (isSessionsMapInitialized) {
			return CallbackToFutureAdapter.getFuture { completer ->
				completer.set(sessions.values.filter { it.isActive })
				tag
			}
		}
		return initializeSessions(tag) { sessions -> sessions.filter { it.isActive } }
	}

	private fun getSessionFromDb(sessionId: UUID, completer: Completer<Session<UninstallFailure>?>) {
		sessions[sessionId]?.let { session ->
			completer.set(session)
			return
		}
		val session = uninstallSessionDao.getUninstallSession(sessionId.toString())
		val uninstallSession = session?.toUninstallSession()?.let { sessions.putIfAbsent(sessionId, it) ?: it }
		completer.set(uninstallSession)
	}

	private inline fun initializeSessions(
		caller: String,
		crossinline transform: SessionsCollectionTransformer
	) = CallbackToFutureAdapter.getFuture { completer ->
		executor.executeWithCompleter(completer) {
			val sessions = initializeSessions()
			completer.set(transform(sessions))
		}
		"$caller -> PackageUninstallerImpl.initializeSessions"
	}

	private fun initializeSessions(): Collection<Session<UninstallFailure>> {
		if (isSessionsMapInitialized) {
			return sessions.values
		}
		uninstallSessionDao.getUninstallSessions()
			.asSequence()
			.filterNot { session ->
				sessions.containsKey(UUID.fromString(session.session.id))
			}
			.forEach { session ->
				val uninstallSession = session.toUninstallSession()
				sessions.putIfAbsent(uninstallSession.id, uninstallSession)
			}
		isSessionsMapInitialized = true
		return sessions.values
	}

	private fun persistSession(
		id: UUID,
		parameters: UninstallParameters,
		dbWriteSemaphore: BinarySemaphore,
		notificationId: Int
	) = executor.executeWithSemaphore(dbWriteSemaphore) {
		val notificationData = uninstallSessionFactory.resolveNotificationData(
			parameters.notificationData,
			parameters.packageName
		)
		uninstallSessionDao.insertUninstallSession(
			SessionEntity.UninstallSession(
				session = SessionEntity(
					id.toString(),
					SessionEntity.Type.UNINSTALL,
					SessionEntity.State.PENDING,
					parameters.confirmation,
					notificationData.title,
					notificationData.contentText,
					notificationData.icon,
					requireUserAction = true
				),
				packageName = parameters.packageName,
				notificationId
			)
		)
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
			initialState = session.state.toSessionState(session.id, uninstallSessionDao),
			notificationId!!, BinarySemaphore()
		)
	}
}