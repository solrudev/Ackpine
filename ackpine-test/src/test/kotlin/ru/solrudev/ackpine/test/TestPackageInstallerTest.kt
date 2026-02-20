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

package ru.solrudev.ackpine.test

import kotlinx.coroutines.test.runTest
import ru.solrudev.ackpine.installer.InstallFailure
import ru.solrudev.ackpine.installer.getActiveSessions
import ru.solrudev.ackpine.installer.getSession
import ru.solrudev.ackpine.installer.getSessions
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.session.await
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TestPackageInstallerTest {

	@Test
	fun createSessionStoresSessionParametersAndPreservesProvidedIdContract() {
		val installer = TestPackageInstaller()
		val parameters = installParameters()

		val session = installer.createSession(parameters)

		assertContains(installer.sessions, session)
		assertEquals(parameters, installer.createdParameters[session.id])
	}

	@Test
	fun badFactoryIdMismatchThrowsIllegalStateException() {
		val installer = TestPackageInstaller { _, _ -> TestInstallSession(id = UUID.randomUUID()) }
		val exception = assertFailsWith<IllegalStateException> {
			installer.createSession(installParameters())
		}
		val message = assertNotNull(exception.message)
		assertContains(message, "Session factory must return session with the provided id=")
	}

	@Test
	fun constructorWithCustomScriptCreatesIndependentPerSessionScriptCopies() {
		val script = TestSessionScript.empty<InstallFailure>()
			.onLaunch(Session.State.Awaiting)
			.onCommit(Session.State.Succeeded)
			.onCancel(Session.State.Cancelled)
		val installer = TestPackageInstaller(script)

		val first = installer.createSession(installParameters())
		val second = installer.createSession(installParameters())

		assertNotSame(script, first.controller.script)
		assertNotSame(script, second.controller.script)
		assertEquals(script, first.controller.script)
		assertEquals(script, second.controller.script)
	}

	@Test
	fun getSessionReturnsExpectedData() = runTest {
		val installer = TestPackageInstaller()
		val first = installer.createSession(installParameters())
		val second = installer.createSession(installParameters())

		assertEquals(first, installer.getSession(first.id))
		assertEquals(second, installer.getSession(second.id))
		assertNull(installer.getSession(UUID.randomUUID()))
	}

	@Test
	fun getSessionsReturnsExpectedData() = runTest {
		val installer = TestPackageInstaller()
		val first = installer.createSession(installParameters())
		val second = installer.createSession(installParameters())
		assertEquals(listOf(first, second), installer.getSessions())
	}

	@Test
	fun getActiveSessionsReturnsExpectedData() = runTest {
		val installer = TestPackageInstaller()
		val first = installer.createSession(installParameters())
		val second = installer.createSession(installParameters())

		first.launch()
		second.await()

		assertEquals(listOf(first), installer.getActiveSessions())
	}

	@Test
	fun seedSessionIgnoresDuplicateId() {
		val installer = TestPackageInstaller()
		val id = UUID.randomUUID()
		val first = TestInstallSession(id = id)
		val second = TestInstallSession(id = id, initialState = Session.State.Succeeded)

		installer.seedSession(first)
		installer.seedSession(second)

		assertEquals(listOf(first), installer.sessions)
		assertTrue(installer.createdParameters.isEmpty())
	}

	@Test
	fun removeSessionRemovesSessionAndCreatedParameters() {
		val installer = TestPackageInstaller()
		val parameters = installParameters()
		val session = installer.createSession(parameters)
		assertEquals(parameters, installer.createdParameters[session.id])

		installer.removeSession(session.id)

		assertTrue(installer.sessions.isEmpty())
		assertTrue(installer.createdParameters.isEmpty())
	}

	@Test
	fun clearSessionsClearsAllRepositories() {
		val installer = TestPackageInstaller()
		installer.createSession(installParameters())
		installer.createSession(installParameters())

		installer.clearSessions()

		assertTrue(installer.sessions.isEmpty())
		assertTrue(installer.createdParameters.isEmpty())
	}
}