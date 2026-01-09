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

import com.google.common.util.concurrent.ListenableFuture
import ru.solrudev.ackpine.installer.InstallFailure
import ru.solrudev.ackpine.installer.PackageInstaller
import ru.solrudev.ackpine.installer.parameters.InstallParameters
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.test.futures.ImmediateFuture
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * An in-memory [PackageInstaller] test double.
 *
 * Sessions are stored in memory and returned via [ImmediateFuture] from async accessors, which makes it suitable
 * for deterministic JVM tests. Use [seedSession] or [createSession] with a predefined ID when you need stable
 * session identifiers.
 *
 * By default, creates sessions with [TestSessionScript.auto], completing with [Session.State.Succeeded].
 */
public class TestPackageInstaller @JvmOverloads public constructor(
	private val sessionFactory: TestInstallSessionFactory = TestInstallSessionFactory.withScript(
		TestSessionScript.auto(Session.State.Succeeded)
	)
) : PackageInstaller {

	/**
	 * Returns a [TestPackageInstaller] which creates all sessions with the provided [script].
	 */
	public constructor(script: TestSessionScript<InstallFailure>) : this(
		TestInstallSessionFactory.withScript(script)
	)

	private val sessions = ConcurrentHashMap<UUID, TestInstallSession>()
	private val sessionsValues = CopyOnWriteArrayList<TestInstallSession>()

	override fun createSession(parameters: InstallParameters): TestInstallSession {
		val id = UUID.randomUUID()
		val session = sessionFactory.create(id, parameters)
		sessions[id] = session
		sessionsValues += session
		return session
	}

	/**
	 * Creates and tracks a session with a predefined [sessionId].
	 */
	public fun createSession(sessionId: UUID, parameters: InstallParameters): TestInstallSession {
		val session = sessionFactory.create(sessionId, parameters)
		sessions[sessionId] = session
		sessionsValues += session
		return session
	}

	override fun getSessionAsync(sessionId: UUID): ListenableFuture<TestInstallSession?> {
		return ImmediateFuture.success(sessions[sessionId])
	}

	override fun getSessionsAsync(): ListenableFuture<List<TestInstallSession>> {
		return ImmediateFuture.success(sessions.values.toList())
	}

	override fun getActiveSessionsAsync(): ListenableFuture<List<TestInstallSession>> {
		val activeSessions = sessions.values.filter { it.isActive }
		return ImmediateFuture.success(activeSessions)
	}

	/**
	 * Adds an existing [session] to this repository.
	 */
	public fun seedSession(session: TestInstallSession): TestInstallSession {
		sessions[session.id] = session
		sessionsValues += session
		return session
	}

	/**
	 * Returns a snapshot of all tracked sessions in the order they were added/created.
	 */
	public fun getSessions(): List<TestInstallSession> = sessionsValues.toList()

	/**
	 * Clears all sessions in this repository.
	 */
	public fun clearSessions() {
		sessions.clear()
		sessionsValues.clear()
	}
}