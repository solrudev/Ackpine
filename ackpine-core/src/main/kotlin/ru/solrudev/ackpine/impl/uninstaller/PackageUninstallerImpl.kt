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

package ru.solrudev.ackpine.impl.uninstaller

import android.content.Context
import android.os.Handler
import androidx.annotation.RestrictTo
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.concurrent.futures.CallbackToFutureAdapter.Completer
import com.google.common.util.concurrent.ListenableFuture
import ru.solrudev.ackpine.Ackpine
import ru.solrudev.ackpine.AckpineThreadPool
import ru.solrudev.ackpine.impl.database.AckpineDatabase
import ru.solrudev.ackpine.impl.database.dao.UninstallSessionDao
import ru.solrudev.ackpine.impl.database.model.SessionEntity
import ru.solrudev.ackpine.impl.helpers.concurrent.BinarySemaphore
import ru.solrudev.ackpine.impl.helpers.concurrent.computeIfAbsentCompat
import ru.solrudev.ackpine.impl.helpers.executeWithCompleter
import ru.solrudev.ackpine.impl.helpers.executeWithSemaphore
import ru.solrudev.ackpine.impl.session.CompletableSession
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
			(Collection<CompletableSession<UninstallFailure>>) -> List<CompletableSession<UninstallFailure>>

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class PackageUninstallerImpl internal constructor(
	private val uninstallSessionDao: UninstallSessionDao,
	private val executor: Executor,
	private val uninstallSessionFactory: UninstallSessionFactory,
	private val uuidFactory: () -> UUID,
	private val notificationIdFactory: () -> Int
) : PackageUninstaller {

	private val sessions = ConcurrentHashMap<UUID, CompletableSession<UninstallFailure>>()

	@Volatile
	private var isSessionsMapInitialized = false

	override fun createSession(parameters: UninstallParameters): CompletableSession<UninstallFailure> {
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
			getSession(sessionId, completer)
		}
		"PackageUninstallerImpl.getSessionAsync($sessionId)"
	}

	override fun getSessionsAsync() = getSessionsAsync(
		caller = "PackageUninstallerImpl.getSessionsAsync",
		transform = { sessions -> sessions.toList() }
	)

	override fun getActiveSessionsAsync() = getSessionsAsync(
		caller = "PackageUninstallerImpl.getActiveSessionsAsync",
		transform = { sessions -> sessions.filter { it.isActive } }
	)

	private fun getSession(sessionId: UUID, completer: Completer<CompletableSession<UninstallFailure>?>) {
		val session = sessions.computeIfAbsentCompat(sessionId) {
			uninstallSessionDao
				.getUninstallSession(sessionId.toString())
				?.toUninstallSession()
		}
		completer.set(session)
	}

	private inline fun getSessionsAsync(
		caller: String,
		crossinline transform: SessionsCollectionTransformer
	): ListenableFuture<List<CompletableSession<UninstallFailure>>> {
		if (isSessionsMapInitialized) {
			return CallbackToFutureAdapter.getFuture { completer ->
				completer.set(transform(sessions.values))
				caller
			}
		}
		return CallbackToFutureAdapter.getFuture { completer ->
			executor.executeWithCompleter(completer) {
				val sessions = initializeSessions()
				completer.set(transform(sessions))
			}
			"$caller -> initializing sessions"
		}
	}

	private fun initializeSessions(): Collection<CompletableSession<UninstallFailure>> {
		if (isSessionsMapInitialized) {
			return sessions.values
		}
		uninstallSessionDao.getUninstallSessions()
			.asSequence()
			.filterNot { session ->
				sessions.containsKey(UUID.fromString(session.session.id))
			}
			.forEach { session ->
				val id = UUID.fromString(session.session.id)
				sessions.computeIfAbsentCompat(id) { session.toUninstallSession() }
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

	private fun SessionEntity.UninstallSession.toUninstallSession(): CompletableSession<UninstallFailure> {
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

	internal companion object {

		private val lock = Any()

		@Volatile
		private var packageUninstaller: PackageUninstallerImpl? = null

		// Hide from Java and don't mangle the function name so it can be linked to in ackpine-api
		@JvmName("getInstance")
		@JvmSynthetic
		internal fun getInstance(context: Context): PackageUninstallerImpl {
			var instance = packageUninstaller
			if (instance != null) {
				return instance
			}
			synchronized(lock) {
				instance = packageUninstaller
				if (instance == null) {
					instance = create(context)
					packageUninstaller = instance
				}
			}
			return instance!!
		}

		private fun create(context: Context): PackageUninstallerImpl {
			val database = AckpineDatabase.getInstance(context.applicationContext, AckpineThreadPool)
			return PackageUninstallerImpl(
				database.uninstallSessionDao(),
				AckpineThreadPool,
				UninstallSessionFactoryImpl(
					context.applicationContext,
					database.sessionDao(),
					database.uninstallSessionDao(),
					AckpineThreadPool,
					Handler(context.mainLooper)
				),
				uuidFactory = UUID::randomUUID,
				notificationIdFactory = Ackpine.globalNotificationId::incrementAndGet
			)
		}
	}
}