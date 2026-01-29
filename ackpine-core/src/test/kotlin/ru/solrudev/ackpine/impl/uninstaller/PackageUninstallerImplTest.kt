/*
 * Copyright (C) 2026 Ilya Fomichev
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

import android.os.Build
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.solrudev.ackpine.impl.HasAckpineDatabaseTest
import ru.solrudev.ackpine.impl.database.model.SessionEntity
import ru.solrudev.ackpine.impl.helpers.concurrent.BinarySemaphore
import ru.solrudev.ackpine.impl.plugability.AckpineServiceProviders
import ru.solrudev.ackpine.impl.testutil.ImmediateExecutor
import ru.solrudev.ackpine.impl.testutil.TestCompletableSession
import ru.solrudev.ackpine.impl.testutil.createUninstallSessionEntity
import ru.solrudev.ackpine.impl.testutil.toSessionState
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.session.parameters.NotificationData
import ru.solrudev.ackpine.uninstaller.UninstallFailure
import ru.solrudev.ackpine.uninstaller.createSession
import ru.solrudev.ackpine.uninstaller.getActiveSessions
import ru.solrudev.ackpine.uninstaller.getSession
import ru.solrudev.ackpine.uninstaller.getSessions
import ru.solrudev.ackpine.uninstaller.parameters.UninstallParameters
import ru.solrudev.ackpine.uninstaller.parameters.UninstallerType
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class PackageUninstallerImplTest : HasAckpineDatabaseTest() {

	@Test
	fun createSessionStoresInMemory() = runTest {
		val factory = FakeUninstallSessionFactory()
		val uninstaller = createUninstaller(factory)

		val session = uninstaller.createSession("com.example.app")
		val sessionFromMap = uninstaller.getSession(session.id)

		assertSame(session, sessionFromMap)
		assertEquals(listOf(session.id), factory.createdIds)
	}

	@Test
	fun getSessionAsyncLoadsFromDaoWhenAbsentInMemory() = runTest {
		val sessionId = UUID.randomUUID()
		val entity = createUninstallSessionEntity(
			id = sessionId.toString(),
			state = SessionEntity.State.PENDING,
			uninstallerType = UninstallerType.DEFAULT,
			packageName = "com.example.app"
		)
		database.uninstallSessionDao().insertUninstallSession(entity)
		val factory = FakeUninstallSessionFactory()
		val uninstaller = createUninstaller(factory)

		val session = uninstaller.getSession(sessionId)

		assertNotNull(session)
		assertEquals(listOf(sessionId), factory.createdIds)
	}

	@Test
	fun getActiveSessionsAsyncFiltersInactive() = runTest {
		val activeId = UUID.randomUUID()
		database.uninstallSessionDao().insertUninstallSession(
			createUninstallSessionEntity(
				id = activeId.toString(),
				state = SessionEntity.State.ACTIVE,
				uninstallerType = UninstallerType.DEFAULT,
				packageName = "com.example.app"
			)
		)
		database.uninstallSessionDao().insertUninstallSession(
			createUninstallSessionEntity(
				id = UUID.randomUUID().toString(),
				state = SessionEntity.State.PENDING,
				uninstallerType = UninstallerType.DEFAULT,
				packageName = "com.example.app"
			)
		)
		database.uninstallSessionDao().insertUninstallSession(
			createUninstallSessionEntity(
				id = UUID.randomUUID().toString(),
				state = SessionEntity.State.SUCCEEDED,
				uninstallerType = UninstallerType.DEFAULT,
				packageName = "com.example.app"
			)
		)
		database.uninstallSessionDao().insertUninstallSession(
			createUninstallSessionEntity(
				id = UUID.randomUUID().toString(),
				state = SessionEntity.State.CANCELLED,
				uninstallerType = UninstallerType.DEFAULT,
				packageName = "com.example.app"
			)
		)

		val uninstaller = createUninstaller()
		val activeSessions = uninstaller.getActiveSessions()

		assertEquals(listOf(activeId), activeSessions.map { it.id })
	}

	@Test
	fun getSessionsAsyncReturnsAllSessionsFromDatabase() = runTest {
		val sessionId1 = UUID.randomUUID()
		val sessionId2 = UUID.randomUUID()
		val sessionId3 = UUID.randomUUID()
		database.uninstallSessionDao().insertUninstallSession(
			createUninstallSessionEntity(
				id = sessionId1.toString(),
				state = SessionEntity.State.PENDING,
				uninstallerType = UninstallerType.INTENT_BASED,
				packageName = "com.example.app1"
			)
		)
		database.uninstallSessionDao().insertUninstallSession(
			createUninstallSessionEntity(
				id = sessionId2.toString(),
				state = SessionEntity.State.AWAITING,
				uninstallerType = UninstallerType.PACKAGE_INSTALLER_BASED,
				packageName = "com.example.app2"
			)
		)
		database.uninstallSessionDao().insertUninstallSession(
			createUninstallSessionEntity(
				id = sessionId3.toString(),
				state = SessionEntity.State.SUCCEEDED,
				uninstallerType = UninstallerType.PACKAGE_INSTALLER_BASED,
				packageName = "com.example.app2"
			)
		)

		val uninstaller = createUninstaller()
		val sessions = uninstaller.getSessions()

		assertEquals(3, sessions.size)
		assertEquals(setOf(sessionId1, sessionId2, sessionId3), sessions.map { it.id }.toSet())
	}

	@Test
	fun getSessionsAsyncReturnsEmptyWhenNoSessions() = runTest {
		val uninstaller = createUninstaller()
		val sessions = uninstaller.getSessions()
		assertTrue(sessions.isEmpty())
	}

	@Test
	fun getSessionAsyncReturnsNullForNonExistentSession() = runTest {
		val uninstaller = createUninstaller()
		val session = uninstaller.getSession(UUID.randomUUID())
		assertNull(session)
	}

	private fun createUninstaller(
		factory: UninstallSessionFactory = FakeUninstallSessionFactory()
	) = PackageUninstallerImpl(
		uninstallSessionDao = database.uninstallSessionDao(),
		executor = ImmediateExecutor,
		ackpineServiceProviders = AckpineServiceProviders(lazy { emptySet() }),
		uninstallSessionFactory = factory,
		uuidFactory = UUID::randomUUID,
		notificationIdFactory = { 1 }
	)

	private class FakeUninstallSessionFactory : UninstallSessionFactory {

		val createdIds = mutableListOf<UUID>()

		override fun create(
			parameters: UninstallParameters,
			id: UUID,
			initialState: Session.State<UninstallFailure>,
			notificationId: Int,
			dbWriteSemaphore: BinarySemaphore
		) = TestCompletableSession(id, initialState).also { createdIds += id }

		override fun create(uninstallSession: SessionEntity.UninstallSession) = TestCompletableSession(
			UUID.fromString(uninstallSession.session.id),
			initialState = uninstallSession.session.state.toSessionState<UninstallFailure> { UninstallFailure.Generic("fail") }
		).also { createdIds += it.id }

		override fun resolveNotificationData(
			notificationData: NotificationData,
			packageName: String
		): NotificationData = notificationData
	}
}