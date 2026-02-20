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
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.session.await
import ru.solrudev.ackpine.uninstaller.UninstallFailure
import ru.solrudev.ackpine.uninstaller.getActiveSessions
import ru.solrudev.ackpine.uninstaller.getSession
import ru.solrudev.ackpine.uninstaller.getSessions
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TestPackageUninstallerTest {

	@Test
	fun createSessionStoresSessionParametersAndPreservesProvidedIdContract() {
		val uninstaller = TestPackageUninstaller()
		val parameters = uninstallParameters()

		val session = uninstaller.createSession(parameters)

		assertContains(uninstaller.sessions, session)
		assertEquals(parameters, uninstaller.createdParameters[session.id])
	}

	@Test
	fun badFactoryIdMismatchThrowsIllegalStateException() {
		val uninstaller = TestPackageUninstaller { _, _ -> TestUninstallSession(id = UUID.randomUUID()) }
		val exception = assertFailsWith<IllegalStateException> {
			uninstaller.createSession(uninstallParameters())
		}
		val message = assertNotNull(exception.message)
		assertContains(message, "Session factory must return session with the provided id=")
	}

	@Test
	fun constructorWithCustomScriptCreatesIndependentPerSessionScriptCopies() {
		val script = TestSessionScript.empty<UninstallFailure>()
			.onLaunch(Session.State.Awaiting)
			.onCommit(Session.State.Succeeded)
			.onCancel(Session.State.Cancelled)
		val uninstaller = TestPackageUninstaller(script)

		val first = uninstaller.createSession(uninstallParameters("com.example.one"))
		val second = uninstaller.createSession(uninstallParameters("com.example.two"))

		assertNotSame(script, first.controller.script)
		assertNotSame(script, second.controller.script)
		assertEquals(script, first.controller.script)
		assertEquals(script, second.controller.script)
	}

	@Test
	fun getSessionReturnsExpectedData() = runTest {
		val uninstaller = TestPackageUninstaller()
		val first = uninstaller.createSession(uninstallParameters("com.example.one"))
		val second = uninstaller.createSession(uninstallParameters("com.example.two"))

		assertEquals(first, uninstaller.getSession(first.id))
		assertEquals(second, uninstaller.getSession(second.id))
		assertNull(uninstaller.getSession(UUID.randomUUID()))
	}

	@Test
	fun getSessionsReturnsExpectedData() = runTest {
		val uninstaller = TestPackageUninstaller()
		val first = uninstaller.createSession(uninstallParameters("com.example.one"))
		val second = uninstaller.createSession(uninstallParameters("com.example.two"))
		assertEquals(listOf(first, second), uninstaller.getSessions())
	}

	@Test
	fun getActiveSessionsReturnsExpectedData() = runTest {
		val uninstaller = TestPackageUninstaller()
		val first = uninstaller.createSession(uninstallParameters("com.example.one"))
		val second = uninstaller.createSession(uninstallParameters("com.example.two"))

		first.launch()
		second.await()

		assertEquals(listOf(first), uninstaller.getActiveSessions())
	}

	@Test
	fun seedSessionIgnoresOnDuplicateId() {
		val uninstaller = TestPackageUninstaller()
		val id = UUID.randomUUID()
		val first = TestUninstallSession(id = id)
		val second = TestUninstallSession(id = id, initialState = Session.State.Succeeded)

		uninstaller.seedSession(first)
		uninstaller.seedSession(second)

		assertEquals(listOf(first), uninstaller.sessions)
		assertTrue(uninstaller.createdParameters.isEmpty())
	}

	@Test
	fun removeSessionRemovesSessionAndCreatedParameters() {
		val uninstaller = TestPackageUninstaller()
		val parameters = uninstallParameters()
		val session = uninstaller.createSession(parameters)
		assertEquals(parameters, uninstaller.createdParameters[session.id])

		uninstaller.removeSession(session.id)

		assertTrue(uninstaller.sessions.isEmpty())
		assertTrue(uninstaller.createdParameters.isEmpty())
	}

	@Test
	fun clearSessionsClearsAllRepositories() {
		val uninstaller = TestPackageUninstaller()
		uninstaller.createSession(uninstallParameters("com.example.one"))
		uninstaller.createSession(uninstallParameters("com.example.two"))

		uninstaller.clearSessions()

		assertTrue(uninstaller.sessions.isEmpty())
		assertTrue(uninstaller.createdParameters.isEmpty())
	}
}