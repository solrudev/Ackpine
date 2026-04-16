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

package ru.solrudev.ackpine.impl.installer

import android.net.Uri
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
import ru.solrudev.ackpine.impl.testutil.TestCompletableProgressSession
import ru.solrudev.ackpine.impl.testutil.createInstallSessionEntity
import ru.solrudev.ackpine.impl.testutil.toSessionState
import ru.solrudev.ackpine.installer.InstallFailure
import ru.solrudev.ackpine.installer.createSession
import ru.solrudev.ackpine.installer.getActiveSessions
import ru.solrudev.ackpine.installer.getSession
import ru.solrudev.ackpine.installer.getSessions
import ru.solrudev.ackpine.installer.parameters.InstallParameters
import ru.solrudev.ackpine.installer.parameters.InstallerType
import ru.solrudev.ackpine.session.ProgressSession
import ru.solrudev.ackpine.session.parameters.NotificationData
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class PackageInstallerImplTest : HasAckpineDatabaseTest() {

	@Test
	fun createSessionStoresInMemory() = runTest {
		val installer = createInstaller()

		val session = installer.createSession(Uri.EMPTY)
		val sessionFromMap = installer.getSession(session.id)

		assertSame(session, sessionFromMap)
	}

	@Test
	fun getSessionAsyncLoadsFromDbWhenAbsentInMemory() = runTest {
		val sessionId = insertInstallSession(state = SessionEntity.State.PENDING)
		val factory = FakeInstallSessionFactory()
		val installer = createInstaller(factory)

		val session = installer.getSession(sessionId)
		val sameSession = installer.getSession(sessionId)

		assertNotNull(session)
		assertSame(session, sameSession)
		assertContains(factory.restoredIds, session.id)
	}

	@Test
	fun getActiveSessionsAsyncFiltersInactive() = runTest {
		val activeId1 = insertInstallSession(state = SessionEntity.State.ACTIVE)
		val activeId2 = insertInstallSession(state = SessionEntity.State.AWAITING)
		val activeId3 = insertInstallSession(state = SessionEntity.State.COMMITTED)
		insertInstallSession(state = SessionEntity.State.PENDING)
		insertInstallSession(state = SessionEntity.State.SUCCEEDED)
		insertInstallSession(state = SessionEntity.State.CANCELLED)

		val installer = createInstaller()
		val activeSessions = installer.getActiveSessions().map { it.id }

		val expectedActiveSessions = setOf(activeId1, activeId2, activeId3)
		assertEquals(3, activeSessions.size)
		assertEquals(expectedActiveSessions, activeSessions.toSet())
	}

	@Test
	fun getSessionsAsyncReturnsAllSessionsFromDatabase() = runTest {
		val expectedSessions = mutableSetOf<UUID>()
		expectedSessions += insertInstallSession(state = SessionEntity.State.PENDING)
		expectedSessions += insertInstallSession(state = SessionEntity.State.ACTIVE)
		expectedSessions += insertInstallSession(state = SessionEntity.State.AWAITING)
		expectedSessions += insertInstallSession(state = SessionEntity.State.COMMITTED)
		expectedSessions += insertInstallSession(state = SessionEntity.State.SUCCEEDED)
		expectedSessions += insertInstallSession(state = SessionEntity.State.CANCELLED)

		val installer = createInstaller()
		val sessions = installer.getSessions().map { it.id }

		assertEquals(6, sessions.size)
		assertEquals(expectedSessions, sessions.toSet())
	}

	@Test
	fun constructorEagerlyRestoresCommittedSessions() = runTest {
		insertInstallSession(state = SessionEntity.State.SUCCEEDED)
		insertInstallSession(state = SessionEntity.State.ACTIVE)
		val committedSessionBased = insertInstallSession(
			state = SessionEntity.State.COMMITTED,
			installerType = InstallerType.SESSION_BASED,
			lastCommitTimestamp = 100
		)
		val committedIntentBasedNewest = insertInstallSession(
			state = SessionEntity.State.COMMITTED,
			installerType = InstallerType.INTENT_BASED,
			lastCommitTimestamp = 90
		)
		val committedIntentBasedOlder = insertInstallSession(
			state = SessionEntity.State.COMMITTED,
			installerType = InstallerType.INTENT_BASED,
			lastCommitTimestamp = 80
		)
		val factory = FakeInstallSessionFactory()

		createInstaller(factory)

		val expectedRestoredSessions = mapOf(
			committedSessionBased to true,
			committedIntentBasedNewest to true,
			committedIntentBasedOlder to false
		)
		val actualRestoreCalls = factory.restoreCalls.associate { it.sessionId to it.completeIfSucceeded }
		assertEquals(expectedRestoredSessions, actualRestoreCalls)
	}

	@Test
	fun getSessionsAsyncReusesInMemorySessionsAndRestoresRemaining() = runTest {
		getSessionsReusesInMemorySessionsAndRestoresRemaining { installer ->
			installer.getSessions()
		}
	}

	@Test
	fun getActiveSessionsAsyncReusesInMemorySessionsAndRestoresRemaining() = runTest {
		getSessionsReusesInMemorySessionsAndRestoresRemaining { installer ->
			installer.getActiveSessions()
		}
	}

	private suspend inline fun getSessionsReusesInMemorySessionsAndRestoresRemaining(
		getSessions: (PackageInstallerImpl) -> List<ProgressSession<InstallFailure>>
	) {
		val inMemoryId = insertInstallSession(state = SessionEntity.State.ACTIVE)
		val sessionId1 = insertInstallSession(state = SessionEntity.State.AWAITING)
		val sessionId2 = insertInstallSession(state = SessionEntity.State.ACTIVE)
		val factory = FakeInstallSessionFactory()
		val installer = createInstaller(factory)

		val restoredSession = installer.getSession(inMemoryId)
		assertNotNull(restoredSession)

		val sessions = getSessions(installer).associateBy { it.id }
		val sameSessions = getSessions(installer).associateBy { it.id }

		assertEquals(setOf(inMemoryId, sessionId1, sessionId2), sessions.keys)
		assertSame(restoredSession, sessions.getValue(inMemoryId))
		assertSame(sessions.getValue(inMemoryId), sameSessions.getValue(inMemoryId))
		assertSame(sessions.getValue(sessionId1), sameSessions.getValue(sessionId1))
		assertSame(sessions.getValue(sessionId2), sameSessions.getValue(sessionId2))
	}

	@Test
	fun getSessionsAsyncReturnsEmptyWhenNoSessions() = runTest {
		val installer = createInstaller()
		val sessions = installer.getSessions()
		assertTrue(sessions.isEmpty())
	}

	@Test
	fun getSessionAsyncReturnsNullForNonExistentSession() = runTest {
		val installer = createInstaller()
		val session = installer.getSession(UUID.randomUUID())
		assertNull(session)
	}

	private fun createInstaller(
		factory: FakeInstallSessionFactory = FakeInstallSessionFactory(),
		logger: RecordingAckpineLogger? = null
	) = PackageInstallerImpl(
		installSessionDao = database.installSessionDao(),
		executor = ImmediateExecutor,
		ackpineServiceProviders = AckpineServiceProviders(
			lazy { emptySet() },
			AckpineLoggerProvider("AckpineServiceProviders") { logger }
		),
		installSessionFactory = factory,
		uuidFactory = UUID::randomUUID,
		notificationIdFactory = { 1 },
		loggerProvider = AckpineLoggerProvider("PackageInstallerImpl") { logger }
	)

	private fun insertInstallSession(
		id: UUID = UUID.randomUUID(),
		state: SessionEntity.State,
		installerType: InstallerType = InstallerType.DEFAULT,
		lastCommitTimestamp: Long = 0,
		lastUpdateTimestamp: Long? = null,
		packageName: String? = null
	): UUID {
		val entity = createInstallSessionEntity(
			id = id.toString(),
			state = state,
			installerType = installerType,
			uris = listOf(""),
			packageName = packageName,
			lastUpdateTimestamp = lastUpdateTimestamp
		)
		database.installSessionDao().insertInstallSession(entity)
		if (lastCommitTimestamp != 0L) {
			database.sessionDao().updateLastCommitTimestamp(entity.session.id, lastCommitTimestamp)
		}
		return id
	}

	private class FakeInstallSessionFactory : InstallSessionFactory {

		val restoredIds: List<UUID>
			get() = _restoreCalls.map { it.sessionId }

		private val _restoreCalls = mutableListOf<RestoreCall>()
		val restoreCalls: List<RestoreCall> = _restoreCalls

		override fun create(
			parameters: InstallParameters,
			id: UUID,
			notificationId: Int,
			dbWriteSemaphore: BinarySemaphore
		) = TestCompletableProgressSession<InstallFailure>(id)

		override fun create(
			session: SessionEntity.InstallSession,
			completeIfSucceeded: Boolean
		): TestCompletableProgressSession<InstallFailure> {
			val testSession = TestCompletableProgressSession(
				UUID.fromString(session.session.id),
				initialState = session.session.state.toSessionState<InstallFailure> {
					InstallFailure.Generic("fail")
				}
			)
			_restoreCalls += RestoreCall(testSession.id, completeIfSucceeded)
			return testSession
		}

		override fun resolveNotificationData(
			notificationData: NotificationData,
			name: String
		) = notificationData

		data class RestoreCall(val sessionId: UUID, val completeIfSucceeded: Boolean)
	}
}