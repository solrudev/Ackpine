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

import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import ru.solrudev.ackpine.impl.HasAckpineDatabaseTest
import ru.solrudev.ackpine.impl.database.model.SessionEntity
import ru.solrudev.ackpine.impl.helpers.concurrent.BinarySemaphore
import ru.solrudev.ackpine.impl.logging.AckpineLoggerProvider
import ru.solrudev.ackpine.impl.plugability.AckpineServiceProviders
import ru.solrudev.ackpine.impl.testutil.ImmediateExecutor
import ru.solrudev.ackpine.impl.testutil.RecordingAckpineLogger
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
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class PackageUninstallerImplTest : HasAckpineDatabaseTest() {

	@Test
	fun createSessionStoresInMemory() = runTest {
		val uninstaller = createUninstaller()

		val session = uninstaller.createSession("com.example.app")
		val sessionFromMap = uninstaller.getSession(session.id)

		assertSame(session, sessionFromMap)
	}

	@Test
	fun getSessionAsyncLoadsFromDbWhenAbsentInMemory() = runTest {
		val sessionId = insertUninstallSession(state = SessionEntity.State.PENDING)
		val factory = FakeUninstallSessionFactory()
		val uninstaller = createUninstaller(factory)

		val session = uninstaller.getSession(sessionId)
		val sameSession = uninstaller.getSession(sessionId)

		assertNotNull(session)
		assertSame(session, sameSession)
		assertContains(factory.restoredIds, session.id)
	}

	@Test
	fun getActiveSessionsAsyncFiltersInactive() = runTest {
		val activeId1 = insertUninstallSession(state = SessionEntity.State.ACTIVE)
		val activeId2 = insertUninstallSession(state = SessionEntity.State.AWAITING)
		val activeId3 = insertUninstallSession(state = SessionEntity.State.COMMITTED)
		insertUninstallSession(state = SessionEntity.State.PENDING)
		insertUninstallSession(state = SessionEntity.State.SUCCEEDED)
		insertUninstallSession(state = SessionEntity.State.CANCELLED)

		val uninstaller = createUninstaller()
		val activeSessions = uninstaller.getActiveSessions().map { it.id }

		assertEquals(3, activeSessions.size)
		val expectedActiveSessions = setOf(activeId1, activeId2, activeId3)
		assertEquals(expectedActiveSessions, activeSessions.toSet())
	}

	@Test
	fun getSessionsAsyncReturnsAllSessionsFromDatabase() = runTest {
		val expectedSessions = mutableSetOf<UUID>()
		expectedSessions += insertUninstallSession(state = SessionEntity.State.PENDING)
		expectedSessions += insertUninstallSession(state = SessionEntity.State.ACTIVE)
		expectedSessions += insertUninstallSession(state = SessionEntity.State.AWAITING)
		expectedSessions += insertUninstallSession(state = SessionEntity.State.COMMITTED)
		expectedSessions += insertUninstallSession(state = SessionEntity.State.SUCCEEDED)
		expectedSessions += insertUninstallSession(state = SessionEntity.State.CANCELLED)

		val uninstaller = createUninstaller()
		val sessions = uninstaller.getSessions().map { it.id }

		assertEquals(6, sessions.size)
		assertEquals(expectedSessions, sessions.toSet())
	}

	@Test
	fun getSessionsAsyncReusesInMemorySessionsAndRestoresRemaining() = runTest {
		getSessionsReusesInMemorySessionsAndRestoresRemaining { uninstaller ->
			uninstaller.getSessions()
		}
	}

	@Test
	fun getActiveSessionsAsyncReusesInMemorySessionsAndRestoresRemaining() = runTest {
		getSessionsReusesInMemorySessionsAndRestoresRemaining { uninstaller ->
			uninstaller.getActiveSessions()
		}
	}

	private suspend inline fun getSessionsReusesInMemorySessionsAndRestoresRemaining(
		getSessions: (PackageUninstallerImpl) -> List<Session<UninstallFailure>>
	) {
		val inMemoryId = insertUninstallSession(state = SessionEntity.State.ACTIVE)
		val sessionId1 = insertUninstallSession(state = SessionEntity.State.AWAITING)
		val sessionId2 = insertUninstallSession(state = SessionEntity.State.ACTIVE)
		val factory = FakeUninstallSessionFactory()
		val uninstaller = createUninstaller(factory)

		val restoredSession = uninstaller.getSession(inMemoryId)
		assertNotNull(restoredSession)

		val sessions = getSessions(uninstaller).associateBy { it.id }
		val sameSessions = getSessions(uninstaller).associateBy { it.id }

		assertEquals(setOf(inMemoryId, sessionId1, sessionId2), sessions.keys)
		assertSame(restoredSession, sessions.getValue(inMemoryId))
		assertSame(sessions.getValue(inMemoryId), sameSessions.getValue(inMemoryId))
		assertSame(sessions.getValue(sessionId1), sameSessions.getValue(sessionId1))
		assertSame(sessions.getValue(sessionId2), sameSessions.getValue(sessionId2))
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
		factory: UninstallSessionFactory = FakeUninstallSessionFactory(),
		logger: RecordingAckpineLogger? = null
	) = PackageUninstallerImpl(
		uninstallSessionDao = database.uninstallSessionDao(),
		executor = ImmediateExecutor,
		ackpineServiceProviders = AckpineServiceProviders(
			lazy { emptySet() },
			AckpineLoggerProvider("AckpineServiceProviders") { logger }
		),
		uninstallSessionFactory = factory,
		uuidFactory = UUID::randomUUID,
		notificationIdFactory = { 1 },
		loggerProvider = AckpineLoggerProvider("PackageUninstallerImpl") { logger }
	)

	private fun insertUninstallSession(
		id: UUID = UUID.randomUUID(),
		state: SessionEntity.State,
		uninstallerType: UninstallerType = UninstallerType.DEFAULT,
		packageName: String = "com.example.app"
	): UUID {
		val entity = createUninstallSessionEntity(
			id = id.toString(),
			state = state,
			uninstallerType = uninstallerType,
			packageName = packageName
		)
		database.uninstallSessionDao().insertUninstallSession(entity)
		return id
	}

	private class FakeUninstallSessionFactory : UninstallSessionFactory {

		private val _restoredIds = mutableListOf<UUID>()
		val restoredIds: List<UUID> = _restoredIds

		override fun create(
			parameters: UninstallParameters,
			id: UUID,
			initialState: Session.State<UninstallFailure>,
			notificationId: Int,
			dbWriteSemaphore: BinarySemaphore
		) = TestCompletableSession(id, initialState)

		override fun create(
			uninstallSession: SessionEntity.UninstallSession
		) = TestCompletableSession(
			UUID.fromString(uninstallSession.session.id),
			initialState = uninstallSession.session.state.toSessionState<UninstallFailure> {
				UninstallFailure.Generic("fail")
			}
		).also { _restoredIds += it.id }

		override fun resolveNotificationData(
			notificationData: NotificationData,
			packageName: String
		): NotificationData = notificationData
	}
}