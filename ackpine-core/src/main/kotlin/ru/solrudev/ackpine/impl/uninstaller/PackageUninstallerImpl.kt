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

import android.annotation.SuppressLint
import androidx.annotation.RestrictTo
import androidx.concurrent.futures.ResolvableFuture
import com.google.common.util.concurrent.ListenableFuture
import ru.solrudev.ackpine.helpers.BinarySemaphore
import ru.solrudev.ackpine.helpers.executeWithFuture
import ru.solrudev.ackpine.helpers.executeWithSemaphore
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
		val semaphore = BinarySemaphore()
		val session = uninstallSessionFactory.create(
			parameters, id,
			initialState = Session.State.Pending,
			notificationId, semaphore
		)
		sessions[id] = session
		persistSession(id, parameters, semaphore, notificationId)
		return session
	}

	@SuppressLint("RestrictedApi")
	override fun getSessionAsync(sessionId: UUID): ListenableFuture<Session<UninstallFailure>?> {
		val future = ResolvableFuture.create<Session<UninstallFailure>?>()
		sessions[sessionId]?.let(future::set) ?: executor.executeWithFuture(future) {
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
		if (isSessionsMapInitialized) {
			return ResolvableFuture.create<List<Session<UninstallFailure>>>().apply {
				set(sessions.values.toList())
			}
		}
		return initializeSessions { sessions -> sessions.toList() }
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
		executor.executeWithFuture(future) {
			for (session in uninstallSessionDao.getUninstallSessions()) {
				if (!sessions.containsKey(UUID.fromString(session.session.id))) {
					val uninstallSession = session.toUninstallSession()
					sessions.putIfAbsent(uninstallSession.id, uninstallSession)
				}
			}
			isSessionsMapInitialized = true
			future.set(transform(sessions.values))
		}
		return future
	}

	private fun persistSession(
		id: UUID,
		parameters: UninstallParameters,
		semaphore: BinarySemaphore,
		notificationId: Int
	) = executor.executeWithSemaphore(semaphore) {
		uninstallSessionDao.insertUninstallSession(
			SessionEntity.UninstallSession(
				session = SessionEntity(
					id.toString(),
					SessionEntity.Type.UNINSTALL,
					SessionEntity.State.PENDING,
					parameters.confirmation,
					parameters.notificationData.title,
					parameters.notificationData.contentText,
					parameters.notificationData.icon,
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