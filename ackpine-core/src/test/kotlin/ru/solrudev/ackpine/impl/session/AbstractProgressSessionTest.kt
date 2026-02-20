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

package ru.solrudev.ackpine.impl.session

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import ru.solrudev.ackpine.DisposableSubscriptionContainer
import ru.solrudev.ackpine.DummyDisposableSubscription
import ru.solrudev.ackpine.impl.helpers.concurrent.BinarySemaphore
import ru.solrudev.ackpine.impl.testutil.ImmediateExecutor
import ru.solrudev.ackpine.impl.testutil.RecordingSessionDao
import ru.solrudev.ackpine.impl.testutil.RecordingSessionProgressDao
import ru.solrudev.ackpine.impl.testutil.TestFailure
import ru.solrudev.ackpine.impl.testutil.TestSessionFailureDao
import ru.solrudev.ackpine.impl.testutil.captureProgress
import ru.solrudev.ackpine.impl.testutil.idleMainThread
import ru.solrudev.ackpine.session.Progress
import ru.solrudev.ackpine.session.ProgressSession
import ru.solrudev.ackpine.session.Session
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class AbstractProgressSessionTest {

	private val context: Context = ApplicationProvider.getApplicationContext()
	private val handler = Handler(Looper.getMainLooper())
	private val dbWriteSemaphore = BinarySemaphore()

	@Test
	fun progressListenerReceivesCurrentProgress() {
		val session = TestProgressSession(initialProgress = Progress(7, 100))
		session.updateProgress(42)
		val progressEvents = session.captureProgress()
		assertEquals(listOf(Progress(42, 100)), progressEvents)
	}

	@Test
	fun setProgressUpdatesListenersAndDao() {
		val progressDao = RecordingSessionProgressDao()
		val session = TestProgressSession(
			progressDao = progressDao,
			initialProgress = Progress(0, 100)
		)
		val progressEvents = session.captureProgress()

		session.updateProgress(42)
		idleMainThread()

		assertEquals(listOf(Progress(0, 100), Progress(42, 100)), progressEvents)
		assertEquals(listOf(Progress(42, 100)), progressDao.progressUpdates.values.last())
	}

	@Test
	fun removeProgressListenerStopsUpdates() {
		val session = TestProgressSession(initialProgress = Progress(0, 100))
		val progressEvents = mutableListOf<Progress>()
		val listener = ProgressSession.ProgressListener { _, progress ->
			progressEvents += progress
		}

		session.addProgressListener(DisposableSubscriptionContainer(), listener)
		idleMainThread()
		session.removeProgressListener(listener)
		session.updateProgress(10)
		idleMainThread()

		assertEquals(listOf(Progress(0, 100)), progressEvents)
	}

	@Test
	fun addDuplicateProgressListenerReturnsDummy() {
		val session = TestProgressSession(initialProgress = Progress(0, 100))
		val container = DisposableSubscriptionContainer()
		val listener = ProgressSession.ProgressListener { _, _ -> }

		val first = session.addProgressListener(container, listener)
		val dummy = session.addProgressListener(container, listener)

		assertNotEquals(DummyDisposableSubscription, first)
		assertEquals(DummyDisposableSubscription, dummy)
	}

	@Test
	fun disposeProgressSubscriptionRemovesListener() {
		val session = TestProgressSession(initialProgress = Progress(0, 100))
		val progressEvents = mutableListOf<Progress>()
		val container = DisposableSubscriptionContainer()
		val listener = ProgressSession.ProgressListener { _, progress ->
			progressEvents += progress
		}

		val subscription = session.addProgressListener(container, listener)
		idleMainThread()
		subscription.dispose()

		session.updateProgress(50)
		idleMainThread()

		assertEquals(listOf(Progress(0, 100)), progressEvents)
		assertTrue(subscription.isDisposed)
	}

	@Test
	fun multipleProgressListenersAllNotified() {
		val session = TestProgressSession(initialProgress = Progress(0, 100))
		val progress1 = session.captureProgress()
		val progress2 = session.captureProgress()
		idleMainThread()

		session.updateProgress(25)
		idleMainThread()

		assertEquals(progress1, progress2)
		assertEquals(listOf(Progress(0, 100), Progress(25, 100)), progress1)
	}

	@Test
	fun sameProgressValueNotNotified() {
		val session = TestProgressSession(initialProgress = Progress(50, 100))
		val progressEvents = session.captureProgress()

		session.updateProgress(50)
		idleMainThread()

		assertEquals(listOf(Progress(50, 100)), progressEvents)
	}

	private inner class TestProgressSession(
		initialProgress: Progress,
		initialState: Session.State<TestFailure> = Session.State.Pending,
		progressDao: RecordingSessionProgressDao = RecordingSessionProgressDao()
	) : AbstractProgressSession<TestFailure>(
		context = context,
		id = UUID.randomUUID(),
		initialState = initialState,
		initialProgress = initialProgress,
		sessionDao = RecordingSessionDao(),
		sessionFailureDao = TestSessionFailureDao(),
		sessionProgressDao = progressDao,
		executor = ImmediateExecutor,
		handler = handler,
		exceptionalFailureFactory = TestFailure::Exceptional,
		notificationId = 1,
		dbWriteSemaphore = dbWriteSemaphore
	) {
		override fun launchConfirmation() { // no-op
		}

		override fun prepare() = notifyAwaiting()
		fun updateProgress(value: Int) = setProgress(value)
	}
}