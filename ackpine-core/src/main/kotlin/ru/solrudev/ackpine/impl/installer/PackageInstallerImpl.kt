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

package ru.solrudev.ackpine.impl.installer

import android.annotation.SuppressLint
import androidx.annotation.RestrictTo
import androidx.concurrent.futures.ResolvableFuture
import androidx.core.net.toUri
import com.google.common.util.concurrent.ListenableFuture
import ru.solrudev.ackpine.helpers.BinarySemaphore
import ru.solrudev.ackpine.helpers.executeWithFuture
import ru.solrudev.ackpine.helpers.executeWithSemaphore
import ru.solrudev.ackpine.helpers.withBinarySemaphore
import ru.solrudev.ackpine.impl.database.dao.InstallSessionDao
import ru.solrudev.ackpine.impl.database.dao.SessionProgressDao
import ru.solrudev.ackpine.impl.database.model.InstallModeEntity
import ru.solrudev.ackpine.impl.database.model.SessionEntity
import ru.solrudev.ackpine.impl.session.toSessionState
import ru.solrudev.ackpine.installer.InstallFailure
import ru.solrudev.ackpine.installer.PackageInstaller
import ru.solrudev.ackpine.installer.parameters.InstallMode
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
	private val installSessionFactory: InstallSessionFactory,
	private val uuidFactory: () -> UUID,
	private val notificationIdFactory: () -> Int
) : PackageInstaller {

	private val sessions = ConcurrentHashMap<UUID, ProgressSession<InstallFailure>>()
	private val committedSessionsInitSemaphore = BinarySemaphore()

	@Volatile
	private var isSessionsMapInitialized = false

	@Volatile
	private var areCommittedSessionsInitialized = false

	init {
		// If app is killed while installing but system installer activity remains visible,
		// session is stuck in Committed state after new process start.
		// We initialize sessions in Committed state eagerly, so that they can complete themselves
		// if they are in fact completed. There shouldn't be many of these sessions.
		initializeCommittedSessions()
	}

	override fun createSession(parameters: InstallParameters): ProgressSession<InstallFailure> {
		val id = uuidFactory()
		val notificationId = notificationIdFactory()
		val dbWriteSemaphore = BinarySemaphore()
		val session = installSessionFactory.create(
			parameters, id,
			initialState = Session.State.Pending,
			initialProgress = Progress(),
			notificationId, dbWriteSemaphore
		)
		sessions[id] = session
		persistSession(id, parameters, dbWriteSemaphore, notificationId)
		return session
	}

	@SuppressLint("RestrictedApi")
	override fun getSessionAsync(sessionId: UUID): ListenableFuture<ProgressSession<InstallFailure>?> {
		val future = ResolvableFuture.create<ProgressSession<InstallFailure>?>()
		sessions[sessionId]?.let(future::set) ?: executor.executeWithFuture(future) {
			if (areCommittedSessionsInitialized) {
				getSessionFromDb(sessionId, future)
			} else {
				committedSessionsInitSemaphore.withBinarySemaphore {
					getSessionFromDb(sessionId, future)
				}
			}
		}
		return future
	}

	@SuppressLint("RestrictedApi")
	override fun getSessionsAsync(): ListenableFuture<List<ProgressSession<InstallFailure>>> {
		if (isSessionsMapInitialized) {
			return ResolvableFuture.create<List<ProgressSession<InstallFailure>>>().apply {
				set(sessions.values.toList())
			}
		}
		return initializeSessions { sessions -> sessions.toList() }
	}

	@SuppressLint("RestrictedApi")
	override fun getActiveSessionsAsync(): ListenableFuture<List<ProgressSession<InstallFailure>>> {
		if (isSessionsMapInitialized) {
			return ResolvableFuture.create<List<ProgressSession<InstallFailure>>>().apply {
				set(sessions.values.filter { it.isActive })
			}
		}
		return initializeSessions { sessions -> sessions.filter { it.isActive } }
	}

	private fun initializeCommittedSessions() = executor.executeWithSemaphore(committedSessionsInitSemaphore) {
		for (session in installSessionDao.getCommittedInstallSessions()) {
			val installSession = session.toInstallSession()
			sessions[installSession.id] = installSession
		}
		areCommittedSessionsInitialized = true
	}

	@SuppressLint("RestrictedApi")
	private fun getSessionFromDb(sessionId: UUID, future: ResolvableFuture<ProgressSession<InstallFailure>>) {
		val session = installSessionDao.getInstallSession(sessionId.toString())
		val installSession = session?.toInstallSession()?.let { sessions.putIfAbsent(sessionId, it) ?: it }
		future.set(installSession)
	}

	@SuppressLint("RestrictedApi")
	private inline fun initializeSessions(
		crossinline transform: (Iterable<ProgressSession<InstallFailure>>) -> List<ProgressSession<InstallFailure>>
	): ListenableFuture<List<ProgressSession<InstallFailure>>> {
		val future = ResolvableFuture.create<List<ProgressSession<InstallFailure>>>()
		executor.executeWithFuture(future) {
			committedSessionsInitSemaphore.withBinarySemaphore {
				for (session in installSessionDao.getInstallSessions()) {
					if (!sessions.containsKey(UUID.fromString(session.session.id))) {
						val installSession = session.toInstallSession()
						sessions.putIfAbsent(installSession.id, installSession)
					}
				}
				isSessionsMapInitialized = true
				future.set(transform(sessions.values))
			}
		}
		return future
	}

	private fun persistSession(
		id: UUID,
		parameters: InstallParameters,
		dbWriteSemaphore: BinarySemaphore,
		notificationId: Int
	) {
		var packageName: String? = null
		val installMode = when (parameters.installMode) {
			is InstallMode.Full -> InstallModeEntity.InstallMode.FULL
			is InstallMode.InheritExisting -> {
				packageName = parameters.installMode.packageName
				InstallModeEntity.InstallMode.INHERIT_EXISTING
			}
		}
		executor.executeWithSemaphore(dbWriteSemaphore) {
			installSessionDao.insertInstallSession(
				SessionEntity.InstallSession(
					session = SessionEntity(
						id.toString(),
						SessionEntity.Type.INSTALL,
						SessionEntity.State.PENDING,
						parameters.confirmation,
						parameters.notificationData.title,
						parameters.notificationData.contentText,
						parameters.notificationData.icon,
						parameters.requireUserAction
					),
					installerType = parameters.installerType,
					uris = parameters.apks.toList().map { it.toString() },
					name = parameters.name,
					notificationId, installMode, packageName
				)
			)
		}
	}

	@SuppressLint("NewApi")
	private fun SessionEntity.InstallSession.toInstallSession(): ProgressSession<InstallFailure> {
		val installMode = when (installMode) {
			null -> InstallMode.Full
			InstallModeEntity.InstallMode.FULL -> InstallMode.Full
			InstallModeEntity.InstallMode.INHERIT_EXISTING -> InstallMode.InheritExisting(
				requireNotNull(packageName) { "Package name was null when install mode is INHERIT_EXISTING" }
			)
		}
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
			.apply {
				val name = this@toInstallSession.name
				if (!name.isNullOrEmpty()) {
					setName(name)
				}
			}
			.setRequireUserAction(session.requireUserAction)
			.setInstallMode(installMode)
			.build()
		return installSessionFactory.create(
			parameters,
			UUID.fromString(session.id),
			initialState = session.state.toSessionState(session.id, installSessionDao),
			initialProgress = sessionProgressDao.getProgress(session.id) ?: Progress(),
			notificationId!!, BinarySemaphore()
		)
	}
}