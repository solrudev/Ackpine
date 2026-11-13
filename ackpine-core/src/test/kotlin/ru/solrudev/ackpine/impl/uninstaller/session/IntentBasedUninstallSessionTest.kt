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

package ru.solrudev.ackpine.impl.uninstaller.session

import android.app.NotificationManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.core.content.getSystemService
import androidx.test.core.app.ApplicationProvider
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowContextWrapper
import ru.solrudev.ackpine.impl.helpers.SessionIdIntents
import ru.solrudev.ackpine.impl.helpers.concurrent.BinarySemaphore
import ru.solrudev.ackpine.impl.logging.AckpineLoggerProvider
import ru.solrudev.ackpine.impl.testutil.ImmediateExecutor
import ru.solrudev.ackpine.impl.testutil.RecordingAckpineLogger
import ru.solrudev.ackpine.impl.testutil.RecordingSessionDao
import ru.solrudev.ackpine.impl.testutil.TestSessionFailureDao
import ru.solrudev.ackpine.impl.testutil.drainMainThread
import ru.solrudev.ackpine.impl.uninstaller.activity.IntentBasedUninstallActivity
import ru.solrudev.ackpine.impl.uninstaller.activity.UninstallActivity
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.session.parameters.Confirmation
import ru.solrudev.ackpine.session.parameters.NotificationData
import ru.solrudev.ackpine.uninstaller.UninstallFailure
import ru.solrudev.ackpine.uninstaller.parameters.UninstallVersion
import ru.solrudev.ackpine.uninstaller.parameters.VersionedUninstallPolicy
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val NOTIFICATION_ID = 42

@RunWith(RobolectricTestRunner::class)
class IntentBasedUninstallSessionTest {

	private val context: Context = ApplicationProvider.getApplicationContext()
	private val failureDao = TestSessionFailureDao<UninstallFailure>()

	@Test
	fun commitImmediateLaunchesUninstallActivity() {
		val sessionId = UUID.randomUUID()
		val session = createSession(
			id = sessionId,
			confirmation = Confirmation.IMMEDIATE
		)

		assertTrue(session.commit())

		val started = Shadow.extract<ShadowContextWrapper>(context).nextStartedActivity
		assertNotNull(started)
		assertEquals(IntentBasedUninstallActivity::class.java.name, started.component?.className)
		assertEquals("com.example.app", started.getStringExtra(UninstallActivity.EXTRA_PACKAGE_NAME))
		assertEquals(sessionId, SessionIdIntents.getSessionId(started, tag = "test"))
	}

	@Test
	fun commitDeferredPostsNotification() {
		val sessionId = UUID.randomUUID()
		val session = createSession(
			id = sessionId,
			confirmation = Confirmation.DEFERRED
		)

		assertTrue(session.commit())
		drainMainThread()

		val notificationManager = shadowOf(context.getSystemService<NotificationManager>())
		assertNotNull(notificationManager.getNotification(sessionId.toString(), NOTIFICATION_ID))
	}

	@Test
	fun restoredStrictExactTargetFailsWithoutLaunchingConfirmation() {
		val session = createSession(
			id = UUID.randomUUID(),
			confirmation = Confirmation.IMMEDIATE,
			uninstallVersion = UninstallVersion(42)
		)
		assertTrue(session.commit())
		assertTrue(session.isCompleted)
		assertIs<UninstallFailure.Unsupported>(failureDao.getFailure(session.id.toString()))
		assertNull(Shadow.extract<ShadowContextWrapper>(context).nextStartedActivity)
	}

	@Test
	fun unsafeExactTargetForwardsVersionAndPolicyToConfirmation() {
		val session = createSession(
			id = UUID.randomUUID(),
			confirmation = Confirmation.IMMEDIATE,
			uninstallVersion = UninstallVersion(42, VersionedUninstallPolicy.ALLOW_NAME_ONLY)
		)
		assertTrue(session.commit())
		val started = assertNotNull(Shadow.extract<ShadowContextWrapper>(context).nextStartedActivity)
		assertEquals(42L, started.getLongExtra(UninstallActivity.EXTRA_PACKAGE_VERSION_CODE, -1))
		assertEquals(
			VersionedUninstallPolicy.ALLOW_NAME_ONLY.ordinal,
			started.getIntExtra(UninstallActivity.EXTRA_VERSIONED_UNINSTALL_POLICY, -1)
		)
	}

	private fun createSession(
		id: UUID,
		confirmation: Confirmation,
		uninstallVersion: UninstallVersion = UninstallVersion.HIGHEST,
		initialState: Session.State<UninstallFailure> = Session.State.Awaiting,
		logger: RecordingAckpineLogger? = null
	) = IntentBasedUninstallSession(
		loggerProvider = AckpineLoggerProvider("IntentBasedUninstallSession") { logger },
		context = context,
		packageName = "com.example.app",
		uninstallVersion = uninstallVersion,
		id = id,
		initialState = initialState,
		confirmation = confirmation,
		notificationData = NotificationData.DEFAULT,
		sessionDao = RecordingSessionDao(),
		sessionFailureDao = failureDao,
		executor = ImmediateExecutor,
		handler = Handler(Looper.getMainLooper()),
		notificationId = NOTIFICATION_ID,
		dbWriteSemaphore = BinarySemaphore()
	)
}