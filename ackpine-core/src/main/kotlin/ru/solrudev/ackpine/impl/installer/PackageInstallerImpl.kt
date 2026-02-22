/*
 * Copyright (C) 2023 Ilya Fomichev
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
import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.concurrent.futures.CallbackToFutureAdapter.Completer
import com.google.common.util.concurrent.ListenableFuture
import ru.solrudev.ackpine.Ackpine
import ru.solrudev.ackpine.AckpineThreadPool
import ru.solrudev.ackpine.impl.database.AckpineDatabase
import ru.solrudev.ackpine.impl.database.dao.InstallSessionDao
import ru.solrudev.ackpine.impl.database.model.SessionEntity
import ru.solrudev.ackpine.impl.database.toEntityList
import ru.solrudev.ackpine.impl.helpers.concurrent.BinarySemaphore
import ru.solrudev.ackpine.impl.helpers.concurrent.Locks
import ru.solrudev.ackpine.impl.helpers.concurrent.computeIfAbsentCompat
import ru.solrudev.ackpine.impl.helpers.concurrent.withPermit
import ru.solrudev.ackpine.impl.helpers.executeWithCompleter
import ru.solrudev.ackpine.impl.helpers.executeWithSemaphore
import ru.solrudev.ackpine.impl.plugability.AckpineServiceProviders
import ru.solrudev.ackpine.impl.services.PackageInstallerWrapper
import ru.solrudev.ackpine.impl.session.CompletableProgressSession
import ru.solrudev.ackpine.installer.InstallFailure
import ru.solrudev.ackpine.installer.PackageInstaller
import ru.solrudev.ackpine.installer.parameters.InstallMode
import ru.solrudev.ackpine.installer.parameters.InstallParameters
import ru.solrudev.ackpine.installer.parameters.InstallerType.INTENT_BASED
import ru.solrudev.ackpine.installer.parameters.InstallerType.SESSION_BASED
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor

private typealias SessionsCollectionTransformer =
			(Collection<CompletableProgressSession<InstallFailure>>) -> List<CompletableProgressSession<InstallFailure>>

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class PackageInstallerImpl internal constructor(
	private val installSessionDao: InstallSessionDao,
	private val executor: Executor,
	private val ackpineServiceProviders: AckpineServiceProviders,
	private val installSessionFactory: InstallSessionFactory,
	private val uuidFactory: () -> UUID,
	private val notificationIdFactory: () -> Int
) : PackageInstaller {

	private val sessions = ConcurrentHashMap<UUID, CompletableProgressSession<InstallFailure>>()
	private val committedSessionsInitSemaphore = BinarySemaphore()
	private val sessionLocks = Locks(32)

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

	override fun createSession(parameters: InstallParameters): CompletableProgressSession<InstallFailure> {
		val id = uuidFactory()
		val notificationId = notificationIdFactory()
		val dbWriteSemaphore = BinarySemaphore()
		persistSession(parameters, id, notificationId, dbWriteSemaphore)
		val session = installSessionFactory.create(parameters, id, notificationId, dbWriteSemaphore)
		sessions[id] = session
		return session
	}

	override fun getSessionAsync(sessionId: UUID) = CallbackToFutureAdapter.getFuture { completer ->
		sessions[sessionId]?.let(completer::set) ?: executor.executeWithCompleter(completer) {
			if (areCommittedSessionsInitialized) {
				getSession(sessionId, completer)
			} else {
				committedSessionsInitSemaphore.withPermit {
					getSession(sessionId, completer)
				}
			}
		}
		"PackageInstallerImpl.getSessionAsync($sessionId)"
	}

	override fun getSessionsAsync() = getSessionsAsync(
		caller = "PackageInstallerImpl.getSessionsAsync",
		transform = { sessions -> sessions.toList() }
	)

	override fun getActiveSessionsAsync() = getSessionsAsync(
		caller = "PackageInstallerImpl.getActiveSessionsAsync",
		transform = { sessions -> sessions.filter { it.isActive } }
	)

	/**
	 * Adds [session] to an in-memory map.
	 */
	@VisibleForTesting
	@JvmSynthetic
	internal fun addSession(
		sessionId: UUID,
		session: CompletableProgressSession<InstallFailure>
	) {
		sessions[sessionId] = session
	}

	@SuppressLint("NewApi")
	private fun initializeCommittedSessions() = executor.executeWithSemaphore(committedSessionsInitSemaphore) {
		// InstallSessionDao.getCommittedInstallSessions() list is sorted by last commit timestamp
		// in descending order. We complete only the last committed intent-based session if
		// self-update succeeded. Unfortunately, on Android 10+, if there were multiple installer
		// activities visible before force stop, the activities in back stack under the last one
		// remain visible (behavior observed with Google's system package installer), and we are
		// not able to work around the process stop and determine whether installations launched
		// from them were successful, so they will remain in COMMITTED state. Luckily, it can be
		// believed that this usage scenario is not very probable.
		installSessionDao.getCommittedInstallSessions()
			.groupingBy { it.installerType }
			.aggregate { type, _: Unit?, session, first ->
				val installSession = installSessionFactory.create(
					session,
					completeIfSucceeded = type == SESSION_BASED || first && type == INTENT_BASED
				)
				sessions[installSession.id] = installSession
			}
		areCommittedSessionsInitialized = true
	}

	private fun getSession(sessionId: UUID, completer: Completer<CompletableProgressSession<InstallFailure>?>) {
		val session = sessions.computeIfAbsentCompat(sessionId, sessionLocks) {
			installSessionDao
				.getInstallSession(sessionId.toString())
				?.let(installSessionFactory::create)
		}
		completer.set(session)
	}

	private inline fun getSessionsAsync(
		caller: String,
		crossinline transform: SessionsCollectionTransformer
	): ListenableFuture<List<CompletableProgressSession<InstallFailure>>> {
		if (isSessionsMapInitialized) {
			return CallbackToFutureAdapter.getFuture { completer ->
				completer.set(transform(sessions.values))
				caller
			}
		}
		return CallbackToFutureAdapter.getFuture { completer ->
			executor.executeWithCompleter(completer) {
				committedSessionsInitSemaphore.withPermit {
					val sessions = initializeSessions()
					completer.set(transform(sessions))
				}
			}
			"$caller -> initializing sessions"
		}
	}

	private fun initializeSessions(): Collection<CompletableProgressSession<InstallFailure>> {
		if (isSessionsMapInitialized) {
			return sessions.values
		}
		installSessionDao.getInstallSessions()
			.asSequence()
			.filterNot { session ->
				sessions.containsKey(UUID.fromString(session.session.id))
			}
			.forEach { session ->
				val id = UUID.fromString(session.session.id)
				sessions.computeIfAbsentCompat(id, sessionLocks) { installSessionFactory.create(session) }
			}
		isSessionsMapInitialized = true
		return sessions.values
	}

	private fun persistSession(
		parameters: InstallParameters,
		id: UUID,
		notificationId: Int,
		dbWriteSemaphore: BinarySemaphore
	) = executor.executeWithSemaphore(dbWriteSemaphore) {
		val sessionId = id.toString()
		val packageName = (parameters.installMode as? InstallMode.InheritExisting)?.packageName
		val notificationData = installSessionFactory.resolveNotificationData(
			parameters.notificationData,
			parameters.name
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
				plugins = parameters.pluginContainer.toEntityList(sessionId),
				name = parameters.name,
				notificationId,
				installMode = parameters.installMode.toEntity(sessionId),
				packageName,
				lastUpdateTimestamp = Long.MAX_VALUE,
				preapproval = parameters.preapproval.toEntity(sessionId),
				constraints = parameters.constraints.toEntity(sessionId),
				parameters.requestUpdateOwnership, parameters.packageSource
			)
		)
		ackpineServiceProviders.persistPluginParameters(id, parameters.pluginContainer)
	}

	internal companion object {

		private val lock = Any()

		@SuppressLint("StaticFieldLeak")
		@Volatile
		private var packageInstaller: PackageInstallerImpl? = null

		/**
		 * Returns a singleton instance of [PackageInstallerImpl].
		 */
		// Hide from Java and don't mangle the function name so it can be linked to in ackpine-api
		@JvmName("getInstance")
		@JvmSynthetic
		internal fun getInstance(context: Context): PackageInstallerImpl {
			var instance = packageInstaller
			if (instance != null) {
				return instance
			}
			synchronized(lock) {
				instance = packageInstaller
				if (instance == null) {
					instance = create(context)
					packageInstaller = instance
				}
			}
			return instance!!
		}

		private fun create(context: Context): PackageInstallerImpl {
			val applicationContext = context.applicationContext
			val database = AckpineDatabase.getInstance(applicationContext, AckpineThreadPool)
			val ackpineServiceProviders = AckpineServiceProviders.create(applicationContext)
			return PackageInstallerImpl(
				database.installSessionDao(),
				AckpineThreadPool,
				ackpineServiceProviders,
				InstallSessionFactoryImpl(
					applicationContext,
					@SuppressLint("NewApi")
					PackageInstallerWrapper.default(applicationContext),
					ackpineServiceProviders,
					database.lastUpdateTimestampDao(),
					database.installSessionDao(),
					database.sessionDao(),
					database.sessionProgressDao(),
					database.nativeSessionIdDao(),
					database.installPreapprovalDao(),
					database.installConstraintsDao(),
					AckpineThreadPool,
					Handler(context.mainLooper),
					sessionCallbackHandler()
				),
				uuidFactory = UUID::randomUUID,
				notificationIdFactory = Ackpine.globalNotificationId::incrementAndGet
			)
		}

		private fun sessionCallbackHandler() = lazy {
			Handler(
				HandlerThread("ackpine.session-callback-handler")
					.apply { start() }
					.looper
			)
		}
	}
}