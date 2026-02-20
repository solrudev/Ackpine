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

import ru.solrudev.ackpine.DisposableSubscriptionContainer
import ru.solrudev.ackpine.DummyDisposableSubscription
import ru.solrudev.ackpine.session.Progress
import ru.solrudev.ackpine.session.ProgressSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

class TestProgressSessionTest {

	@Test
	fun progressListenerReceivesCurrentProgress() {
		val session = TestInstallSession()
		val expectedProgress = Progress(42, 100)
		session.updateProgress(expectedProgress)
		val progressEvents = session.captureProgress()
		assertEquals(listOf(expectedProgress), progressEvents)
	}

	@Test
	fun removeProgressListenerStopsUpdates() {
		val session = TestInstallSession(initialProgress = Progress(0, 100))
		val progressEvents = mutableListOf<Progress>()
		val listener = ProgressSession.ProgressListener { _, progress ->
			progressEvents += progress
		}

		session.addProgressListener(DisposableSubscriptionContainer(), listener)
		session.removeProgressListener(listener)
		session.updateProgress(Progress(10, 100))

		assertEquals(listOf(Progress(0, 100)), progressEvents)
	}

	@Test
	fun addDuplicateProgressListenerReturnsDummy() {
		val session = TestInstallSession()
		val container = DisposableSubscriptionContainer()
		val listener = ProgressSession.ProgressListener { _, _ -> }

		val first = session.addProgressListener(container, listener)
		val dummy = session.addProgressListener(container, listener)

		assertNotEquals(DummyDisposableSubscription, first)
		assertEquals(DummyDisposableSubscription, dummy)
	}

	@Test
	fun disposeProgressSubscriptionRemovesListener() {
		val session = TestInstallSession(initialProgress = Progress(0, 100))
		val progressEvents = mutableListOf<Progress>()
		val container = DisposableSubscriptionContainer()
		val listener = ProgressSession.ProgressListener { _, progress ->
			progressEvents += progress
		}

		val subscription = session.addProgressListener(container, listener)
		subscription.dispose()

		session.updateProgress(Progress(50, 100))

		assertEquals(listOf(Progress(0, 100)), progressEvents)
		assertTrue(subscription.isDisposed)
	}

	@Test
	fun multipleProgressListenersAllNotified() {
		val session = TestInstallSession(initialProgress = Progress(0, 100))
		val progress1 = session.captureProgress()
		val progress2 = session.captureProgress()

		session.updateProgress(Progress(25, 100))

		assertEquals(progress1, progress2)
		assertEquals(listOf(Progress(0, 100), Progress(25, 100)), progress1)
	}

	@Test
	fun sameProgressValueNotNotified() {
		val session = TestInstallSession(initialProgress = Progress(50, 100))
		val progressEvents = session.captureProgress()

		session.updateProgress(Progress(50, 100))

		assertEquals(listOf(Progress(50, 100)), progressEvents)
	}

	@Test
	fun progressHistoryAppendsOnProgressEvents() {
		val session = TestInstallSession(initialProgress = Progress(0, 100))

		session.updateProgress(Progress(11, 100))
		session.updateProgress(Progress(22, 100))
		session.updateProgress(Progress(33, 100))

		val expectedProgress = listOf(
			Progress(0, 100),
			Progress(11, 100),
			Progress(22, 100),
			Progress(33, 100)
		)
		assertEquals(expectedProgress, session.progressHistory)
	}

	@Test
	fun resetResetsProgressHistory() {
		val session = TestInstallSession(initialProgress = Progress(4, 10))
		val progressUpdates = session.captureProgress()
		session.updateProgress(Progress(7, 10))

		session.resetProgress(progress = Progress(2, 10), notifyListeners = false)
		assertEquals(Progress(2, 10), session.progress)
		assertEquals(listOf(Progress(2, 10)), session.progressHistory)
		assertEquals(listOf(Progress(4, 10), Progress(7, 10)), progressUpdates)

		session.resetProgress(progress = Progress(1, 10), notifyListeners = true)
		assertEquals(Progress(1, 10), session.progress)
		assertEquals(listOf(Progress(1, 10)), session.progressHistory)
		val expectedProgress = listOf(
			Progress(4, 10),
			Progress(7, 10),
			Progress(1, 10)
		)
		assertEquals(expectedProgress, progressUpdates)
	}

	@Test
	fun progressHistoryReturnsSnapshotCopy() {
		val session = TestInstallSession()

		val snapshot1 = session.progressHistory
		session.updateProgress(Progress(50, 100))
		val snapshot2 = session.progressHistory

		assertNotSame(snapshot1, snapshot2)
		assertNotEquals(snapshot1, snapshot2)
	}
}