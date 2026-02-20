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

import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import ru.solrudev.ackpine.core.R
import ru.solrudev.ackpine.impl.HasAckpineDatabaseTest
import ru.solrudev.ackpine.impl.database.model.SessionEntity
import ru.solrudev.ackpine.impl.helpers.concurrent.BinarySemaphore
import ru.solrudev.ackpine.impl.installer.session.IntentBasedInstallSession
import ru.solrudev.ackpine.impl.installer.session.SessionBasedInstallSession
import ru.solrudev.ackpine.impl.installer.session.helpers.PROGRESS_MAX
import ru.solrudev.ackpine.impl.plugability.AckpineServiceProviders
import ru.solrudev.ackpine.impl.testutil.ImmediateExecutor
import ru.solrudev.ackpine.impl.testutil.RecordingPackageInstallerService
import ru.solrudev.ackpine.impl.testutil.captureStates
import ru.solrudev.ackpine.impl.testutil.createInstallSessionEntity
import ru.solrudev.ackpine.installer.InstallFailure
import ru.solrudev.ackpine.installer.parameters.InstallParameters
import ru.solrudev.ackpine.installer.parameters.InstallerType
import ru.solrudev.ackpine.resources.ResolvableString
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.session.parameters.NotificationData
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class InstallSessionFactoryImplTest : HasAckpineDatabaseTest() {

	@Test
	fun resolveNotificationDataUsesDefaultStringsWithName() {
		val factory = createFactory()
		val resolved = factory.resolveNotificationData(NotificationData.DEFAULT, name = "My App")
		assertEquals(context.getString(R.string.ackpine_prompt_install_title), resolved.title.resolve(context))
		assertEquals(
			context.getString(R.string.ackpine_prompt_install_message_with_label, "My App"),
			resolved.contentText.resolve(context)
		)
	}

	@Test
	fun resolveNotificationDataUsesDefaultStringsWithoutName() {
		val factory = createFactory()
		val resolved = factory.resolveNotificationData(NotificationData.DEFAULT, name = "")
		assertEquals(context.getString(R.string.ackpine_prompt_install_title), resolved.title.resolve(context))
		assertEquals(
			context.getString(R.string.ackpine_prompt_install_message),
			resolved.contentText.resolve(context)
		)
	}

	@Test
	fun resolveNotificationDataPreservesCustomTitle() {
		val factory = createFactory()
		val customData = NotificationData {
			title = ResolvableString.raw("Custom Title")
		}
		val resolved = factory.resolveNotificationData(customData, name = "My App")
		assertEquals("Custom Title", resolved.title.resolve(context))
		assertEquals(
			context.getString(R.string.ackpine_prompt_install_message_with_label, "My App"),
			resolved.contentText.resolve(context)
		)
	}

	@Test
	fun resolveNotificationDataPreservesCustomContent() {
		val factory = createFactory()
		val customData = NotificationData {
			contentText = ResolvableString.raw("Custom content")
		}
		val resolved = factory.resolveNotificationData(customData, name = "My App")
		assertEquals(context.getString(R.string.ackpine_prompt_install_title), resolved.title.resolve(context))
		assertEquals("Custom content", resolved.contentText.resolve(context))
	}

	@Test
	fun createIntentBasedSessionReturnsIntentBasedInstallSession() {
		val factory = createFactory()
		val params = InstallParameters(Uri.EMPTY) {
			installerType = InstallerType.INTENT_BASED
		}
		val session = factory.create(
			parameters = params,
			id = UUID.randomUUID(),
			notificationId = 1,
			dbWriteSemaphore = BinarySemaphore()
		)
		assertIs<IntentBasedInstallSession>(session)
	}

	@Test
	fun createSessionBasedSessionReturnsSessionBasedInstallSession() {
		val factory = createFactory()
		val params = InstallParameters(Uri.EMPTY) {
			installerType = InstallerType.SESSION_BASED
		}
		val session = factory.create(
			parameters = params,
			id = UUID.randomUUID(),
			notificationId = 1,
			dbWriteSemaphore = BinarySemaphore()
		)
		assertIs<SessionBasedInstallSession>(session)
	}

	@Test
	fun createSessionBasedWithMultipleApksReturnsSessionBasedInstallSession() {
		val factory = createFactory()
		val params = InstallParameters(listOf(Uri.EMPTY, Uri.EMPTY, Uri.EMPTY)) {
			installerType = InstallerType.INTENT_BASED
		}
		val session = factory.create(
			parameters = params,
			id = UUID.randomUUID(),
			notificationId = 1,
			dbWriteSemaphore = BinarySemaphore()
		)
		assertIs<SessionBasedInstallSession>(session)
	}

	@Test
	fun createFromEntityIntentBasedReturnsIntentBasedInstallSession() {
		val factory = createFactory()
		val entity = createInstallSessionEntity(
			id = UUID.randomUUID().toString(),
			state = SessionEntity.State.PENDING,
			installerType = InstallerType.INTENT_BASED,
			uris = listOf("file:///tmp/base.apk")
		)
		val session = factory.create(entity)
		assertIs<IntentBasedInstallSession>(session)
	}

	@Test
	fun createFromEntitySessionBasedReturnsSessionBasedInstallSession() {
		val factory = createFactory()
		val entity = createInstallSessionEntity(
			id = UUID.randomUUID().toString(),
			state = SessionEntity.State.PENDING,
			installerType = InstallerType.SESSION_BASED,
			uris = listOf("file:///tmp/base.apk")
		)
		val session = factory.create(entity)
		assertIs<SessionBasedInstallSession>(session)
	}

	@Test
	fun createFromEntityPreservesInitialState() {
		val stateMappings = listOf(
			SessionEntity.State.PENDING to Session.State.Pending,
			SessionEntity.State.ACTIVE to Session.State.Active,
			SessionEntity.State.AWAITING to Session.State.Awaiting,
			SessionEntity.State.CANCELLED to Session.State.Cancelled,
			SessionEntity.State.SUCCEEDED to Session.State.Succeeded
		)
		val factory = createFactory()
		for ((entityState, expectedSessionState) in stateMappings) {
			val entity = createInstallSessionEntity(
				id = UUID.randomUUID().toString(),
				state = entityState,
				installerType = InstallerType.DEFAULT,
				uris = listOf("file:///tmp/base.apk")
			)
			val session = factory.create(entity)
			val states = session.captureStates()
			assertEquals(expectedSessionState, states.first())
		}
	}

	@Test
	fun createFromEntityPreservesState() {
		val factory = createFactory()
		val entity = createInstallSessionEntity(
			id = UUID.randomUUID().toString(),
			state = SessionEntity.State.COMMITTED,
			installerType = InstallerType.SESSION_BASED,
			uris = listOf("file:///tmp/base.apk"),
			nativeSessionId = 999
		)

		val session = factory.create(entity, completeIfSucceeded = false)
		val states = session.captureStates()

		assertEquals(Session.State.Committed, states.last())
	}

	@Test
	fun intentBasedSelfUpdateSessionCompletesSuccessfully() {
		val newTimestamp = System.currentTimeMillis()
		val oldTimestamp = newTimestamp - 10000
		setSelfUpdateTimestamp(newTimestamp)

		val factory = createFactory()
		val sessionId = UUID.randomUUID().toString()

		val entity = createInstallSessionEntity(
			id = sessionId,
			state = SessionEntity.State.COMMITTED,
			installerType = InstallerType.INTENT_BASED,
			uris = listOf("file:///tmp/base.apk"),
			packageName = context.packageName,
			lastUpdateTimestamp = oldTimestamp
		)
		database.installSessionDao().insertInstallSession(entity)

		val session = factory.create(entity, completeIfSucceeded = true)
		val states = session.captureStates()

		assertEquals(Session.State.Succeeded, states.last())
	}

	@Test
	fun intentBasedSelfUpdateSessionStaysCommittedWhenNotUpdated() {
		val timestamp = System.currentTimeMillis()
		setSelfUpdateTimestamp(timestamp)

		val factory = createFactory()
		val entity = createInstallSessionEntity(
			id = UUID.randomUUID().toString(),
			state = SessionEntity.State.COMMITTED,
			installerType = InstallerType.INTENT_BASED,
			uris = listOf("file:///tmp/base.apk"),
			packageName = context.packageName,
			lastUpdateTimestamp = timestamp // Same timestamp - not updated
		)

		val session = factory.create(entity, completeIfSucceeded = true)
		val states = session.captureStates()

		assertEquals(Session.State.Committed, states.last())
	}

	@Test
	fun intentBasedNonSelfUpdateSessionStaysCommitted() {
		val currentTimestamp = System.currentTimeMillis()
		val oldTimestamp = currentTimestamp - 10000
		setSelfUpdateTimestamp(currentTimestamp)

		val factory = createFactory()
		val entity = createInstallSessionEntity(
			id = UUID.randomUUID().toString(),
			state = SessionEntity.State.COMMITTED,
			installerType = InstallerType.INTENT_BASED,
			uris = listOf("file:///tmp/base.apk"),
			packageName = "com.other.app", // Different package
			lastUpdateTimestamp = oldTimestamp
		)

		val session = factory.create(entity, completeIfSucceeded = true)
		val states = session.captureStates()

		assertEquals(Session.State.Committed, states.last())
	}

	@Test
	fun intentBasedTerminalStateNotModified() = testTerminalStateNotModified(InstallerType.INTENT_BASED)

	@Test
	fun sessionBasedCommittedSessionCompletesSucceededWhenNativeSessionIsGone() {
		val factory = createFactory()
		val entity = createInstallSessionEntity(
			id = UUID.randomUUID().toString(),
			state = SessionEntity.State.COMMITTED,
			installerType = InstallerType.SESSION_BASED,
			uris = listOf("file:///tmp/base.apk"),
			nativeSessionId = 999 // non-existent session
		)

		val session = factory.create(entity, completeIfSucceeded = true)
		val states = session.captureStates()
		shadowOf(Looper.getMainLooper()).runToEndOfTasks()

		assertEquals(Session.State.Succeeded, states.last())
	}

	@Test
	fun sessionBasedCommittedSessionStaysCommittedWhenNativeSessionExists() {
		val packageInstaller = context.packageManager.packageInstaller
		val sessionParams = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
		val nativeSessionId = packageInstaller.createSession(sessionParams)

		val factory = createFactory()
		val entity = createInstallSessionEntity(
			id = UUID.randomUUID().toString(),
			state = SessionEntity.State.COMMITTED,
			installerType = InstallerType.SESSION_BASED,
			uris = listOf("file:///tmp/base.apk"),
			nativeSessionId = nativeSessionId
		)

		val session = factory.create(entity, completeIfSucceeded = true)
		val states = session.captureStates()

		assertEquals(Session.State.Committed, states.last())
	}

	@Test
	fun sessionBasedOngoingInstallDetectedViaProgressBlocksCommitForCommittedSession() {
		val factory = createFactory()
		val sessionId = UUID.randomUUID().toString()
		val commitProgressValue = CommitProgressValueHolder.get(context)
		val highProgress = (commitProgressValue * PROGRESS_MAX).toInt()

		val entity = createInstallSessionEntity(
			id = sessionId,
			state = SessionEntity.State.COMMITTED,
			installerType = InstallerType.SESSION_BASED,
			uris = listOf("file:///tmp/base.apk")
		)
		database.installSessionDao().insertInstallSession(entity)
		database.sessionProgressDao().initProgress(sessionId)
		database.sessionProgressDao().updateProgress(sessionId, highProgress, PROGRESS_MAX)

		val session = factory.create(entity, completeIfSucceeded = true)
		val states = session.captureStates()

		assertEquals(Session.State.Committed, states.last())
		assertFalse(session.commit())
	}

	@Test
	fun sessionBasedOngoingInstallNotDetectedViaProgressDoesNotBlockCommitForCommittedSession() {
		val factory = createFactory()
		val sessionId = UUID.randomUUID().toString()
		val commitProgressValue = CommitProgressValueHolder.get(context)
		val lowProgress = ((commitProgressValue * PROGRESS_MAX) - 10).toInt()

		val entity = createInstallSessionEntity(
			id = sessionId,
			state = SessionEntity.State.COMMITTED,
			installerType = InstallerType.SESSION_BASED,
			uris = listOf("file:///tmp/base.apk")
		)
		database.installSessionDao().insertInstallSession(entity)
		database.sessionProgressDao().initProgress(sessionId)
		database.sessionProgressDao().updateProgress(sessionId, lowProgress, PROGRESS_MAX)

		val session = factory.create(entity, completeIfSucceeded = true)
		val states = session.captureStates()

		assertEquals(Session.State.Committed, states.last())
		assertTrue(session.commit())
	}

	@Test
	fun sessionBasedTerminalStateNotModified() = testTerminalStateNotModified(InstallerType.SESSION_BASED)

	private fun testTerminalStateNotModified(installerType: InstallerType) {
		val factory = createFactory()
		val entity = createInstallSessionEntity(
			id = UUID.randomUUID().toString(),
			state = SessionEntity.State.FAILED,
			installerType,
			uris = listOf("file:///tmp/base.apk")
		)
		val expectedFailure = InstallFailure.Generic("fail")
		database.installSessionDao().insertInstallSession(entity)
		database.installSessionDao().setFailure(entity.session.id, expectedFailure)

		val session = factory.create(entity, completeIfSucceeded = true)

		val state = session.captureStates().last()
		assertIs<Session.State.Failed<InstallFailure>>(state)
		assertEquals(expectedFailure, state.failure)
	}

	@Test
	@Config(sdk = [Build.VERSION_CODES.S, Build.VERSION_CODES.S_V2])
	fun sessionBasedApi31And32WithoutUserActionAndConfirmationLaunchedDoesNotBlockCommit() {
		val factory = createFactory()
		val sessionId = UUID.randomUUID().toString()
		val commitProgressValue = CommitProgressValueHolder.get(context)
		val highProgress = (commitProgressValue * PROGRESS_MAX).toInt()

		val entity = createInstallSessionEntity(
			id = sessionId,
			state = SessionEntity.State.COMMITTED,
			installerType = InstallerType.SESSION_BASED,
			uris = listOf("file:///tmp/base.apk"),
			requireUserAction = false,
			wasConfirmationLaunched = true
		)
		database.installSessionDao().insertInstallSession(entity)
		database.sessionProgressDao().initProgress(sessionId)
		database.sessionProgressDao().updateProgress(sessionId, highProgress, PROGRESS_MAX)

		val session = factory.create(entity, completeIfSucceeded = true)
		val states = session.captureStates()

		assertEquals(Session.State.Committed, states.last())
		assertTrue(session.commit())
	}

	private fun setSelfUpdateTimestamp(timestamp: Long) {
		val packageInfo = PackageInfo().apply {
			packageName = context.packageName
			lastUpdateTime = timestamp
		}
		val shadowPackageManager = shadowOf(context.packageManager)
		shadowPackageManager.installPackage(packageInfo)
	}

	private fun createFactory() = InstallSessionFactoryImpl(
		applicationContext = context,
		defaultPackageInstallerService = lazy { RecordingPackageInstallerService() },
		ackpineServiceProviders = AckpineServiceProviders(lazy { emptySet() }),
		lastUpdateTimestampDao = database.lastUpdateTimestampDao(),
		installSessionDao = database.installSessionDao(),
		sessionDao = database.sessionDao(),
		sessionProgressDao = database.sessionProgressDao(),
		nativeSessionIdDao = database.nativeSessionIdDao(),
		installPreapprovalDao = database.installPreapprovalDao(),
		installConstraintsDao = database.installConstraintsDao(),
		executor = ImmediateExecutor,
		handler = Handler(Looper.getMainLooper()),
		sessionCallbackHandler = lazy { Handler(Looper.getMainLooper()) }
	)
}