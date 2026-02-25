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

package ru.solrudev.ackpine.impl.installer.session

import ru.solrudev.ackpine.impl.helpers.concurrent.BinarySemaphore
import ru.solrudev.ackpine.impl.testutil.RecordingInstallPreapprovalDao
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PreapprovalLifecycleTest {

	@Test
	fun runPreapprovalRequestFromIdleSetsActive() {
		val preapprovalDao = RecordingInstallPreapprovalDao()
		val lifecycle = createLifecycle(
			initialState = PreapprovalLifecycle.State.IDLE,
			preapprovalDao = preapprovalDao
		)

		lifecycle.runPreapprovalRequest {}

		assertFalse(SESSION_ID in preapprovalDao.activatingSessions)
		assertContains(preapprovalDao.activeSessions, SESSION_ID)
		assertTrue(lifecycle.isActive())
		assertFalse(lifecycle.isPreapproved())
	}

	@Test
	fun runPreapprovalRequestWhenAlreadyActiveDoesNothing() {
		val preapprovalDao = RecordingInstallPreapprovalDao()
		val lifecycle = createLifecycle(
			initialState = PreapprovalLifecycle.State.ACTIVE,
			preapprovalDao = preapprovalDao
		)
		var wasCalled = false

		lifecycle.runPreapprovalRequest {
			wasCalled = true
		}

		assertFalse(wasCalled)
		assertTrue(lifecycle.isActive())
		assertTrue(preapprovalDao.activatingSessions.isEmpty())
		assertTrue(preapprovalDao.activeSessions.isEmpty())
	}

	@Test
	fun runPreapprovalRequestFromRestoredActivatingClaimsOnce() {
		val preapprovalDao = RecordingInstallPreapprovalDao()
		preapprovalDao.setActivating(SESSION_ID)
		val lifecycle = createLifecycle(
			initialState = PreapprovalLifecycle.State.ACTIVATING,
			preapprovalDao = preapprovalDao
		)
		var callsCount = 0

		lifecycle.runPreapprovalRequest {
			callsCount++
		}
		lifecycle.runPreapprovalRequest {
			callsCount++
		}

		assertEquals(1, callsCount)
		assertEquals(1, preapprovalDao.activeSessions.count { it == SESSION_ID })
		assertTrue(lifecycle.isActive())
	}

	@Test
	fun runPreapprovalRequestAbortsAndRethrowsOnFailure() {
		val preapprovalDao = RecordingInstallPreapprovalDao()
		val lifecycle = createLifecycle(
			initialState = PreapprovalLifecycle.State.IDLE,
			preapprovalDao = preapprovalDao
		)

		assertFailsWith<RuntimeException> {
			lifecycle.runPreapprovalRequest {
				throw RuntimeException("boom")
			}
		}

		assertFalse(lifecycle.isActive())
		assertFalse(lifecycle.isPreapproved())
		val actualConsumeCalls = preapprovalDao.consumeCalls.count { call ->
			call.sessionId == SESSION_ID && !call.isPreapproved && call.result == 1
		}
		assertEquals(1, actualConsumeCalls)
		assertTrue(preapprovalDao.activeSessions.isEmpty())
	}

	@Test
	fun runPreapprovalRequestWithIllegalStateExceptionFromFreshRequestAbortsAndRethrows() {
		val preapprovalDao = RecordingInstallPreapprovalDao()
		val lifecycle = createLifecycle(
			initialState = PreapprovalLifecycle.State.IDLE,
			preapprovalDao = preapprovalDao
		)

		assertFailsWith<IllegalStateException> {
			lifecycle.runPreapprovalRequest {
				throw IllegalStateException()
			}
		}

		assertFalse(lifecycle.isActive())
		assertFalse(lifecycle.isPreapproved())
		val actualConsumeCalls = preapprovalDao.consumeCalls.count { call ->
			call.sessionId == SESSION_ID && !call.isPreapproved && call.result == 1
		}
		assertEquals(1, actualConsumeCalls)
		assertTrue(preapprovalDao.activeSessions.isEmpty())
	}

	@Test
	fun runPreapprovalRequestWithIllegalStateExceptionFromRestoredActivatingTreatsAsRerequest() {
		val preapprovalDao = RecordingInstallPreapprovalDao()
		preapprovalDao.setActivating(SESSION_ID)
		val lifecycle = createLifecycle(
			initialState = PreapprovalLifecycle.State.ACTIVATING,
			preapprovalDao = preapprovalDao
		)

		lifecycle.runPreapprovalRequest {
			throw IllegalStateException()
		}

		assertFalse(SESSION_ID in preapprovalDao.activatingSessions)
		assertContains(preapprovalDao.activeSessions, SESSION_ID)
		assertTrue(preapprovalDao.consumeCalls.isEmpty())
		assertTrue(lifecycle.isActive())
	}

	@Test
	fun consumeActiveWithPreapprovedTrueIsIdempotent() {
		val preapprovalDao = RecordingInstallPreapprovalDao()
		val lifecycle = createActiveLifecycle(preapprovalDao)

		val firstResult = lifecycle.consumeActive(isPreapproved = true)
		val secondResult = lifecycle.consumeActive(isPreapproved = true)

		assertTrue(firstResult)
		assertFalse(secondResult)
		assertTrue(lifecycle.isPreapproved())
		assertFalse(lifecycle.isActive())
	}

	@Test
	fun consumeActiveWithPreapprovedFalseClearsActive() {
		val preapprovalDao = RecordingInstallPreapprovalDao()
		val lifecycle = createActiveLifecycle(preapprovalDao)

		val result = lifecycle.consumeActive(isPreapproved = false)

		assertTrue(result)
		assertFalse(lifecycle.isActive())
		assertFalse(lifecycle.isPreapproved())
	}

	@Test
	fun consumeActiveWhenIdleReturnsFalse() {
		val lifecycle = createLifecycle(initialState = PreapprovalLifecycle.State.IDLE)

		val result = lifecycle.consumeActive(isPreapproved = false)

		assertFalse(result)
		assertFalse(lifecycle.isActive())
		assertFalse(lifecycle.isPreapproved())
	}

	@Test
	fun resetClearsPreapprovedStateAndAllowsNewRequest() {
		val preapprovalDao = RecordingInstallPreapprovalDao()
		val lifecycle = createActiveLifecycle(preapprovalDao)
		assertTrue(lifecycle.consumeActive(isPreapproved = true))
		assertTrue(lifecycle.isPreapproved())

		lifecycle.reset()

		assertFalse(lifecycle.isActive())
		assertFalse(lifecycle.isPreapproved())
		assertFalse(SESSION_ID in preapprovalDao.activatingSessions)
		assertFalse(SESSION_ID in preapprovalDao.activeSessions)
		assertFalse(SESSION_ID in preapprovalDao.preapprovedSessions)

		lifecycle.runPreapprovalRequest {}

		assertTrue(lifecycle.isActive())
		assertContains(preapprovalDao.activeSessions, SESSION_ID)
		assertFalse(SESSION_ID in preapprovalDao.preapprovedSessions)
	}

	@Test
	fun runPreapprovalRequestDoesNotReactivateAfterStaleFinalize() {
		val preapprovalDao = RecordingInstallPreapprovalDao()
		val lifecycle = createLifecycle(
			initialState = PreapprovalLifecycle.State.IDLE,
			preapprovalDao = preapprovalDao
		)
		var wasCalled = false

		lifecycle.runPreapprovalRequest {
			wasCalled = true
			assertTrue(lifecycle.consumeActive(isPreapproved = false))
		}

		assertTrue(wasCalled)
		assertFalse(lifecycle.isActive())
		assertFalse(lifecycle.isPreapproved())
		assertFalse(SESSION_ID in preapprovalDao.activatingSessions)
		assertFalse(SESSION_ID in preapprovalDao.activeSessions)
		val actualConsumeCalls = preapprovalDao.consumeCalls.count { call ->
			call.sessionId == SESSION_ID && !call.isPreapproved && call.result == 1
		}
		assertEquals(1, actualConsumeCalls)
	}

	private fun createActiveLifecycle(preapprovalDao: RecordingInstallPreapprovalDao): PreapprovalLifecycle {
		return createLifecycle(
			initialState = PreapprovalLifecycle.State.IDLE,
			preapprovalDao = preapprovalDao
		).also { lifecycle ->
			lifecycle.runPreapprovalRequest {}
			assertTrue(lifecycle.isActive())
		}
	}

	private fun createLifecycle(
		initialState: PreapprovalLifecycle.State,
		preapprovalDao: RecordingInstallPreapprovalDao = RecordingInstallPreapprovalDao()
	) = PreapprovalLifecycle(
		initialState = initialState,
		sessionId = SESSION_ID,
		installPreapprovalDao = preapprovalDao,
		dbWriteSemaphore = BinarySemaphore()
	)
}

private const val SESSION_ID = "session-id"