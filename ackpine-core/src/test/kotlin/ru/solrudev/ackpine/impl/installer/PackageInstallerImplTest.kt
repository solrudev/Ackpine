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
import ru.solrudev.ackpine.impl.plugability.AckpineServiceProviders
import ru.solrudev.ackpine.impl.testutil.ImmediateExecutor
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
import ru.solrudev.ackpine.session.parameters.NotificationData
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class PackageInstallerImplTest : HasAckpineDatabaseTest() {

	@Test
	fun createSessionStoresInMemory() = runTest {
		val factory = FakeInstallSessionFactory()
		val installer = createInstaller(factory)

		val session = installer.createSession(Uri.EMPTY)
		val sessionFromMap = installer.getSession(session.id)

		assertSame(session, sessionFromMap)
		assertEquals(listOf(session.id), factory.createdIds)
	}

	@Test
	fun getSessionAsyncLoadsFromDbWhenAbsentInMemory() = runTest {
		val sessionId = UUID.randomUUID()
		val entity = createInstallSessionEntity(
			id = sessionId.toString(),
			state = SessionEntity.State.PENDING,
			installerType = InstallerType.DEFAULT,
			uris = listOf("")
		)
		database.installSessionDao().insertInstallSession(entity)
		val factory = FakeInstallSessionFactory()
		val installer = createInstaller(factory)

		val session = installer.getSession(sessionId)

		assertNotNull(session)
		assertEquals(listOf(sessionId), factory.createdIds)
	}

	@Test
	fun getActiveSessionsAsyncFiltersInactiveAndTerminal() = runTest {
		val activeId = UUID.randomUUID()
		database.installSessionDao().insertInstallSession(
			createInstallSessionEntity(
				id = activeId.toString(),
				state = SessionEntity.State.ACTIVE,
				installerType = InstallerType.DEFAULT,
				uris = listOf("")
			)
		)
		database.installSessionDao().insertInstallSession(
			createInstallSessionEntity(
				id = UUID.randomUUID().toString(),
				state = SessionEntity.State.PENDING,
				installerType = InstallerType.DEFAULT,
				uris = listOf("")
			)
		)
		database.installSessionDao().insertInstallSession(
			createInstallSessionEntity(
				id = UUID.randomUUID().toString(),
				state = SessionEntity.State.SUCCEEDED,
				installerType = InstallerType.DEFAULT,
				uris = listOf("")
			)
		)
		database.installSessionDao().insertInstallSession(
			createInstallSessionEntity(
				id = UUID.randomUUID().toString(),
				state = SessionEntity.State.CANCELLED,
				installerType = InstallerType.DEFAULT,
				uris = listOf("")
			)
		)

		val installer = createInstaller()
		val activeSessions = installer.getActiveSessions()

		assertEquals(listOf(activeId), activeSessions.map { it.id })
	}

	@Test
	fun getSessionsAsyncReturnsAllSessionsFromDatabase() = runTest {
		val sessionId1 = UUID.randomUUID()
		val sessionId2 = UUID.randomUUID()
		val sessionId3 = UUID.randomUUID()
		database.installSessionDao().insertInstallSession(
			createInstallSessionEntity(
				id = sessionId1.toString(),
				state = SessionEntity.State.PENDING,
				installerType = InstallerType.INTENT_BASED,
				uris = listOf("")
			)
		)
		database.installSessionDao().insertInstallSession(
			createInstallSessionEntity(
				id = sessionId2.toString(),
				state = SessionEntity.State.AWAITING,
				installerType = InstallerType.SESSION_BASED,
				uris = listOf("")
			)
		)
		database.installSessionDao().insertInstallSession(
			createInstallSessionEntity(
				id = sessionId3.toString(),
				state = SessionEntity.State.SUCCEEDED,
				installerType = InstallerType.SESSION_BASED,
				uris = listOf("")
			)
		)

		val installer = createInstaller()
		val sessions = installer.getSessions()

		assertEquals(3, sessions.size)
		assertEquals(setOf(sessionId1, sessionId2, sessionId3), sessions.map { it.id }.toSet())
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
		factory: FakeInstallSessionFactory = FakeInstallSessionFactory()
	) = PackageInstallerImpl(
		installSessionDao = database.installSessionDao(),
		executor = ImmediateExecutor,
		ackpineServiceProviders = AckpineServiceProviders(lazy { emptySet() }),
		installSessionFactory = factory,
		uuidFactory = UUID::randomUUID,
		notificationIdFactory = { 1 }
	)

	private class FakeInstallSessionFactory : InstallSessionFactory {

		private val _createdIds = mutableListOf<UUID>()
		val createdIds: List<UUID> = _createdIds

		override fun create(
			parameters: InstallParameters,
			id: UUID,
			notificationId: Int,
			dbWriteSemaphore: BinarySemaphore
		) = TestCompletableProgressSession<InstallFailure>(id).also { _createdIds += id }

		override fun create(
			session: SessionEntity.InstallSession,
			completeIfSucceeded: Boolean
		) = TestCompletableProgressSession(
			UUID.fromString(session.session.id),
			initialState = session.session.state.toSessionState<InstallFailure> { InstallFailure.Generic("fail") }
		).also { _createdIds += it.id }

		override fun resolveNotificationData(
			notificationData: NotificationData,
			name: String
		) = notificationData
	}
}