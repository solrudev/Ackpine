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
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.concurrent.futures.CallbackToFutureAdapter.Completer
import androidx.core.net.toUri
import com.google.common.util.concurrent.ListenableFuture
import ru.solrudev.ackpine.helpers.concurrent.BinarySemaphore
import ru.solrudev.ackpine.helpers.concurrent.executeWithCompleter
import ru.solrudev.ackpine.helpers.concurrent.executeWithSemaphore
import ru.solrudev.ackpine.helpers.concurrent.withPermit
import ru.solrudev.ackpine.impl.database.dao.InstallSessionDao
import ru.solrudev.ackpine.impl.database.dao.SessionProgressDao
import ru.solrudev.ackpine.impl.database.model.InstallConstraintsEntity
import ru.solrudev.ackpine.impl.database.model.InstallModeEntity
import ru.solrudev.ackpine.impl.database.model.InstallPreapprovalEntity
import ru.solrudev.ackpine.impl.database.model.SessionEntity
import ru.solrudev.ackpine.impl.session.toSessionState
import ru.solrudev.ackpine.installer.InstallFailure
import ru.solrudev.ackpine.installer.PackageInstaller
import ru.solrudev.ackpine.installer.parameters.InstallConstraints
import ru.solrudev.ackpine.installer.parameters.InstallMode
import ru.solrudev.ackpine.installer.parameters.InstallParameters
import ru.solrudev.ackpine.installer.parameters.InstallPreapproval
import ru.solrudev.ackpine.installer.parameters.InstallerType
import ru.solrudev.ackpine.installer.parameters.PackageSource
import ru.solrudev.ackpine.session.Progress
import ru.solrudev.ackpine.session.ProgressSession
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.session.parameters.NotificationData
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor

private typealias SessionsCollectionTransformer =
			(Collection<ProgressSession<InstallFailure>>) -> List<ProgressSession<InstallFailure>>

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

	override fun getSessionAsync(sessionId: UUID) = CallbackToFutureAdapter.getFuture { completer ->
		sessions[sessionId]?.let(completer::set) ?: executor.executeWithCompleter(completer) {
			if (areCommittedSessionsInitialized) {
				getSessionFromDb(sessionId, completer)
			} else {
				committedSessionsInitSemaphore.withPermit {
					getSessionFromDb(sessionId, completer)
				}
			}
		}
		"PackageInstallerImpl.getSessionAsync($sessionId)"
	}

	override fun getSessionsAsync(): ListenableFuture<List<ProgressSession<InstallFailure>>> {
		val tag = "PackageInstallerImpl.getSessionsAsync"
		if (isSessionsMapInitialized) {
			return CallbackToFutureAdapter.getFuture { completer ->
				completer.set(sessions.values.toList())
				tag
			}
		}
		return initializeSessions(tag) { sessions -> sessions.toList() }
	}

	override fun getActiveSessionsAsync(): ListenableFuture<List<ProgressSession<InstallFailure>>> {
		val tag = "PackageInstallerImpl.getActiveSessionsAsync"
		if (isSessionsMapInitialized) {
			return CallbackToFutureAdapter.getFuture { completer ->
				completer.set(sessions.values.filter { it.isActive })
				tag
			}
		}
		return initializeSessions(tag) { sessions -> sessions.filter { it.isActive } }
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

	private fun getSessionFromDb(
		sessionId: UUID,
		future: Completer<ProgressSession<InstallFailure>?>
	) {
		sessions[sessionId]?.let { session ->
			future.set(session)
			return
		}
		val session = installSessionDao.getInstallSession(sessionId.toString())
		val installSession = session?.toInstallSession()?.let { sessions.putIfAbsent(sessionId, it) ?: it }
		future.set(installSession)
	}

	private inline fun initializeSessions(
		caller: String,
		crossinline transform: SessionsCollectionTransformer
	) = CallbackToFutureAdapter.getFuture { completer ->
		executor.executeWithCompleter(completer) {
			committedSessionsInitSemaphore.withPermit {
				val sessions = initializeSessions()
				completer.set(transform(sessions))
			}
		}
		"$caller -> PackageInstallerImpl.initializeSessions"
	}

	private fun initializeSessions(): Collection<ProgressSession<InstallFailure>> {
		if (isSessionsMapInitialized) {
			return sessions.values
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
		isSessionsMapInitialized = true
		return sessions.values
	}

	private fun persistSession(
		id: UUID,
		parameters: InstallParameters,
		dbWriteSemaphore: BinarySemaphore,
		notificationId: Int
	) = executor.executeWithSemaphore(dbWriteSemaphore) {
		var packageName: String? = null
		var dontKillApp = false
		val installMode = when (parameters.installMode) {
			is InstallMode.Full -> InstallModeEntity.InstallMode.FULL
			is InstallMode.InheritExisting -> {
				packageName = parameters.installMode.packageName
				dontKillApp = parameters.installMode.dontKillApp
				InstallModeEntity.InstallMode.INHERIT_EXISTING
			}
		}
		val sessionId = id.toString()
		val installModeEntity = InstallModeEntity(sessionId, installMode, dontKillApp)
		val notificationData = installSessionFactory.resolveNotificationData(
			parameters.notificationData,
			parameters.name
		)
		val preapprovalEntity = InstallPreapprovalEntity(
			sessionId,
			parameters.preapproval.packageName,
			parameters.preapproval.label,
			parameters.preapproval.languageTag,
			parameters.preapproval.icon.toString()
		)
		val constraintsEntity = InstallConstraintsEntity(
			sessionId,
			parameters.constraints.isAppNotForegroundRequired,
			parameters.constraints.isAppNotInteractingRequired,
			parameters.constraints.isAppNotTopVisibleRequired,
			parameters.constraints.isDeviceIdleRequired,
			parameters.constraints.isNotInCallRequired,
			parameters.constraints.timeoutMillis,
			parameters.constraints.timeoutStrategy
		)
		installSessionDao.insertInstallSession(
			SessionEntity.InstallSession(
				session = SessionEntity(
					sessionId,
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
				notificationId, installModeEntity, packageName,
				lastUpdateTimestamp = Long.MAX_VALUE,
				preapprovalEntity, constraintsEntity,
				parameters.requestUpdateOwnership, parameters.packageSource
			)
		)
	}

	@SuppressLint("NewApi")
	private fun SessionEntity.InstallSession.toInstallSession(
		needToCompleteIfSucceeded: Boolean = false
	): ProgressSession<InstallFailure> {
		val installMode = when (installMode?.installMode) {
			null -> InstallMode.Full
			InstallModeEntity.InstallMode.FULL -> InstallMode.Full
			InstallModeEntity.InstallMode.INHERIT_EXISTING -> InstallMode.InheritExisting(
				requireNotNull(packageName) { "Package name was null when install mode is INHERIT_EXISTING" },
				installMode.dontKillApp
			)
		}
		val installPreapproval = if (preapproval == null) {
			InstallPreapproval.NONE
		} else {
			InstallPreapproval.Builder(preapproval.packageName, preapproval.label, preapproval.locale)
				.setIcon(preapproval.icon.toUri())
				.build()
		}
		val installConstraints = if (constraints == null) {
			InstallConstraints.NONE
		} else {
			InstallConstraints.Builder(constraints.timeoutMillis)
				.setAppNotForegroundRequired(constraints.isAppNotForegroundRequired)
				.setAppNotInteractingRequired(constraints.isAppNotInteractingRequired)
				.setAppNotTopVisibleRequired(constraints.isAppNotTopVisibleRequired)
				.setDeviceIdleRequired(constraints.isDeviceIdleRequired)
				.setNotInCallRequired(constraints.isNotInCallRequired)
				.setTimeoutStrategy(constraints.timeoutStrategy)
				.build()
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
			.setPreapproval(installPreapproval)
			.setConstraints(installConstraints)
			.setRequestUpdateOwnership(requestUpdateOwnership ?: false)
			.setPackageSource(packageSource ?: PackageSource.Unspecified)
			.build()
		return installSessionFactory.create(
			parameters,
			UUID.fromString(session.id),
			initialState = session.state.toSessionState(session.id, installSessionDao),
			initialProgress = sessionProgressDao.getProgress(session.id) ?: Progress(),
			notificationId!!, BinarySemaphore(),
			InstallSessionFactory.AdditionalParameters(
				packageName = packageName.orEmpty(),
				lastUpdateTimestamp = lastUpdateTimestamp ?: Long.MAX_VALUE,
				needToCompleteIfSucceeded,
				commitAttemptsCount = constraints?.commitAttemptsCount ?: 0,
				isPreapproved = preapproval?.isPreapproved ?: false
			)
		)
	}
}