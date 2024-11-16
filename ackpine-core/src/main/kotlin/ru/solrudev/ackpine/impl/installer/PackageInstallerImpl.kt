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
import ru.solrudev.ackpine.helpers.concurrent.BinarySemaphore
import ru.solrudev.ackpine.helpers.concurrent.executeWithFuture
import ru.solrudev.ackpine.helpers.concurrent.executeWithSemaphore
import ru.solrudev.ackpine.helpers.concurrent.withPermit
import ru.solrudev.ackpine.impl.database.dao.InstallSessionDao
import ru.solrudev.ackpine.impl.database.dao.SessionProgressDao
import ru.solrudev.ackpine.impl.database.model.InstallModeEntity
import ru.solrudev.ackpine.impl.database.model.SessionEntity
import ru.solrudev.ackpine.impl.session.toSessionState
import ru.solrudev.ackpine.installer.InstallFailure
import ru.solrudev.ackpine.installer.PackageInstaller
import ru.solrudev.ackpine.installer.parameters.InstallMode
import ru.solrudev.ackpine.installer.parameters.InstallParameters
import ru.solrudev.ackpine.installer.parameters.InstallerType
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
				committedSessionsInitSemaphore.withPermit {
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

	@SuppressLint("NewApi")
	private fun initializeCommittedSessions() = executor.executeWithSemaphore(committedSessionsInitSemaphore) {
		val intentBasedSessions = mutableListOf<SessionEntity.InstallSession>()
		for (session in installSessionDao.getCommittedInstallSessions()) {
			when (session.installerType) {
				InstallerType.INTENT_BASED -> intentBasedSessions += session
				InstallerType.SESSION_BASED -> {
					val installSession = session.toInstallSession()
					sessions[installSession.id] = installSession
				}
			}
		}
		// InstallSessionDao.getCommittedInstallSessions() list is sorted by last commit timestamp
		// in descending order. We complete only the last committed intent-based session if
		// self-update succeeded. Unfortunately, on Android 10+, if there were multiple installer
		// activities visible before force stop, the activities in back stack under the last one
		// remain visible (behavior observed with Google's system package installer), and we are
		// not able to work around the process stop and determine whether installations launched
		// from them were successful, so they will remain in COMMITTED state. Luckily, it can be
		// believed that this usage scenario is not very probable.
		intentBasedSessions.firstOrNull()
			?.toInstallSession(needToCompleteIfSucceeded = true)
			?.let { installSession ->
				sessions[installSession.id] = installSession
				intentBasedSessions.removeFirst()
			}
		// Initialize remaining committed intent-based sessions anyway
		// so that they can handle their state properly.
		for (session in intentBasedSessions) {
			val installSession = session.toInstallSession()
			sessions[installSession.id] = installSession
		}
		areCommittedSessionsInitialized = true
	}

	@SuppressLint("RestrictedApi")
	private fun getSessionFromDb(sessionId: UUID, future: ResolvableFuture<ProgressSession<InstallFailure>>) {
		sessions[sessionId]?.let { session ->
			future.set(session)
			return
		}
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
			committedSessionsInitSemaphore.withPermit {
				initializeSessions(future, transform)
			}
		}
		return future
	}

	@SuppressLint("RestrictedApi")
	private inline fun initializeSessions(
		future: ResolvableFuture<List<ProgressSession<InstallFailure>>>,
		transform: (Iterable<ProgressSession<InstallFailure>>) -> List<ProgressSession<InstallFailure>>
	) {
		if (isSessionsMapInitialized) {
			return
		}
		installSessionDao.getInstallSessions()
			.asSequence()
			.filterNot { session ->
				sessions.containsKey(UUID.fromString(session.session.id))
			}
			.forEach { session ->
				val installSession = session.toInstallSession()
				sessions.putIfAbsent(installSession.id, installSession)
			}
		}
		isSessionsMapInitialized = true
		future.set(transform(sessions.values))
	}

	private fun persistSession(
		id: UUID,
		parameters: InstallParameters,
		dbWriteSemaphore: BinarySemaphore,
		notificationId: Int
	) = executor.executeWithSemaphore(dbWriteSemaphore) {
		var packageName: String? = null
		val installMode = when (parameters.installMode) {
			is InstallMode.Full -> InstallModeEntity.InstallMode.FULL
			is InstallMode.InheritExisting -> {
				packageName = parameters.installMode.packageName
				InstallModeEntity.InstallMode.INHERIT_EXISTING
			}
		}
		val notificationData = installSessionFactory.resolveNotificationData(
			parameters.notificationData,
			parameters.name
		)
		installSessionDao.insertInstallSession(
			SessionEntity.InstallSession(
				session = SessionEntity(
					id.toString(),
					SessionEntity.Type.INSTALL,
					SessionEntity.State.PENDING,
					parameters.confirmation,
					notificationData.title,
					notificationData.contentText,
					notificationData.icon,
					parameters.requireUserAction
				),
				installerType = parameters.installerType,
				uris = parameters.apks.toList().map { it.toString() },
				name = parameters.name,
				notificationId, installMode, packageName,
				lastUpdateTimestamp = Long.MAX_VALUE
			)
		)
	}

	@SuppressLint("NewApi")
	private fun SessionEntity.InstallSession.toInstallSession(
		needToCompleteIfSucceeded: Boolean = false
	): ProgressSession<InstallFailure> {
		val installMode = when (installMode) {
			null -> InstallMode.Full
			InstallModeEntity.InstallMode.FULL -> InstallMode.Full
			InstallModeEntity.InstallMode.INHERIT_EXISTING -> InstallMode.InheritExisting(
				requireNotNull(packageName) { "Package name was null when install mode is INHERIT_EXISTING" }
			)
		}
		val sessionName = name
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
				if (!sessionName.isNullOrEmpty()) {
					setName(sessionName)
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
			notificationId!!, BinarySemaphore(),
			packageName = packageName.orEmpty(),
			lastUpdateTimestamp = lastUpdateTimestamp ?: Long.MAX_VALUE,
			needToCompleteIfSucceeded
		)
	}
}