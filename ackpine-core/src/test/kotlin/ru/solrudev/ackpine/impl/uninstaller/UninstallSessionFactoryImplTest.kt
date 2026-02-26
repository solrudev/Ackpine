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

package ru.solrudev.ackpine.impl.uninstaller

import android.app.NotificationManager
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import ru.solrudev.ackpine.core.R
import ru.solrudev.ackpine.impl.HasAckpineDatabaseTest
import ru.solrudev.ackpine.impl.database.model.SessionEntity
import ru.solrudev.ackpine.impl.helpers.concurrent.BinarySemaphore
import ru.solrudev.ackpine.impl.plugability.AckpineServiceProviders
import ru.solrudev.ackpine.impl.testutil.ImmediateExecutor
import ru.solrudev.ackpine.impl.testutil.RecordingPackageInstallerService
import ru.solrudev.ackpine.impl.testutil.captureStates
import ru.solrudev.ackpine.impl.testutil.createUninstallSessionEntity
import ru.solrudev.ackpine.impl.uninstaller.session.IntentBasedUninstallSession
import ru.solrudev.ackpine.impl.uninstaller.session.PackageInstallerBasedUninstallSession
import ru.solrudev.ackpine.resources.ResolvableString
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.session.parameters.NotificationData
import ru.solrudev.ackpine.uninstaller.UninstallFailure
import ru.solrudev.ackpine.uninstaller.parameters.UninstallParameters
import ru.solrudev.ackpine.uninstaller.parameters.UninstallerType
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
class UninstallSessionFactoryImplTest : HasAckpineDatabaseTest() {

	@Test
	fun resolveNotificationDataUsesLabelWhenPresent() {
		val packageInfo = PackageInfo().apply {
			packageName = "com.example.app"
			applicationInfo = ApplicationInfo().apply {
				packageName = "com.example.app"
				nonLocalizedLabel = "Example"
			}
		}
		shadowOf(context.packageManager).installPackage(packageInfo)
		val factory = createFactory()

		val resolved = factory.resolveNotificationData(NotificationData.DEFAULT, packageName = "com.example.app")

		assertEquals(context.getString(R.string.ackpine_prompt_uninstall_title), resolved.title.resolve(context))
		assertEquals(
			context.getString(R.string.ackpine_prompt_uninstall_message_with_label, "Example"),
			resolved.contentText.resolve(context)
		)
	}

	@Test
	fun resolveNotificationDataUsesDefaultMessageWithoutLabel() {
		val factory = createFactory()
		val resolved = factory.resolveNotificationData(NotificationData.DEFAULT, packageName = "missing.pkg")
		assertEquals(context.getString(R.string.ackpine_prompt_uninstall_title), resolved.title.resolve(context))
		assertEquals(
			context.getString(R.string.ackpine_prompt_uninstall_message),
			resolved.contentText.resolve(context)
		)
	}

	@Test
	fun resolveNotificationDataPreservesCustomTitle() {
		val factory = createFactory()
		val customData = NotificationData {
			title = ResolvableString.raw("Custom Uninstall Title")
		}
		val resolved = factory.resolveNotificationData(customData, packageName = "com.example.app")
		assertEquals("Custom Uninstall Title", resolved.title.resolve(context))
	}

	@Test
	fun resolveNotificationDataPreservesCustomContent() {
		val factory = createFactory()
		val customData = NotificationData {
			contentText = ResolvableString.raw("Custom uninstall content")
		}
		val resolved = factory.resolveNotificationData(customData, packageName = "com.example.app")
		assertEquals("Custom uninstall content", resolved.contentText.resolve(context))
	}

	@Test
	fun createIntentBasedSessionReturnsIntentBasedUninstallSession() {
		val factory = createFactory()
		val params = UninstallParameters("com.example.app") {
			uninstallerType = UninstallerType.INTENT_BASED
		}
		val session = factory.create(
			parameters = params,
			id = UUID.randomUUID(),
			initialState = Session.State.Pending,
			notificationId = 1,
			dbWriteSemaphore = BinarySemaphore()
		)
		assertIs<IntentBasedUninstallSession>(session)
	}

	@Test
	fun createPackageInstallerBasedSessionReturnsPackageInstallerBasedUninstallSession() {
		val factory = createFactory()
		val params = UninstallParameters("com.example.app") {
			uninstallerType = UninstallerType.PACKAGE_INSTALLER_BASED
		}
		val session = factory.create(
			parameters = params,
			id = UUID.randomUUID(),
			initialState = Session.State.Pending,
			notificationId = 1,
			dbWriteSemaphore = BinarySemaphore()
		)
		assertIs<PackageInstallerBasedUninstallSession>(session)
	}

	@Test
	fun createFromEntityIntentBasedReturnsIntentBasedUninstallSession() {
		val factory = createFactory()
		val entity = createUninstallSessionEntity(
			id = UUID.randomUUID().toString(),
			state = SessionEntity.State.PENDING,
			uninstallerType = UninstallerType.INTENT_BASED,
			packageName = "com.example.app"
		)
		val session = factory.create(entity)
		assertIs<IntentBasedUninstallSession>(session)
	}

	@Test
	fun createFromEntityPackageInstallerBasedReturnsPackageInstallerBasedUninstallSession() {
		val factory = createFactory()
		val entity = createUninstallSessionEntity(
			id = UUID.randomUUID().toString(),
			state = SessionEntity.State.PENDING,
			uninstallerType = UninstallerType.PACKAGE_INSTALLER_BASED,
			packageName = "com.example.app"
		)
		val session = factory.create(entity)
		assertIs<PackageInstallerBasedUninstallSession>(session)
	}

	@Test
	fun createFromEntityPreservesAllStates() {
		val stateMappings = listOf(
			SessionEntity.State.PENDING to Session.State.Pending,
			SessionEntity.State.ACTIVE to Session.State.Active,
			SessionEntity.State.AWAITING to Session.State.Awaiting,
			SessionEntity.State.COMMITTED to Session.State.Committed,
			SessionEntity.State.CANCELLED to Session.State.Cancelled,
			SessionEntity.State.SUCCEEDED to Session.State.Succeeded
		)
		val factory = createFactory()
		for ((entityState, expectedSessionState) in stateMappings) {
			val entity = createUninstallSessionEntity(
				id = UUID.randomUUID().toString(),
				state = entityState,
				uninstallerType = UninstallerType.DEFAULT,
				packageName = "com.example.app"
			)
			val session = factory.create(entity)
			val states = session.captureStates()
			assertEquals(expectedSessionState, states.first())
		}
	}

	@Test
	fun createFromEntityNotificationCancelBehavior() {
		for (state in SessionEntity.State.entries) {
			for (uninstallerType in UninstallerType.entries) {
				context.getSystemService<NotificationManager>()?.cancelAll()
				val sessionId = UUID.randomUUID().toString()
				val notificationId = (state.ordinal + 1) * (uninstallerType.ordinal + 1)
				val entity = createUninstallSessionEntity(
					id = sessionId,
					state = state,
					uninstallerType = uninstallerType,
					packageName = "com.example.app",
					notificationId = notificationId
				)
				database.uninstallSessionDao().insertUninstallSession(entity)
				if (state == SessionEntity.State.FAILED) {
					database.uninstallSessionDao().setFailure(sessionId, UninstallFailure.Generic("failed"))
				}
				showSessionNotification(sessionId, notificationId)

				createFactory().create(entity)

				val expectedNotifications = if (state.isTerminal) 0 else 1
				assertEquals(expectedNotifications, shadowNotificationManager().allNotifications.size)
			}
		}
	}

	private fun showSessionNotification(sessionId: String, notificationId: Int) {
		val manager = context.getSystemService<NotificationManager>()
		assertNotNull(manager)
		val notification = NotificationCompat.Builder(context, "ackpine")
			.setContentTitle("title")
			.setContentText("text")
			.setSmallIcon(android.R.drawable.ic_dialog_alert)
			.build()
		manager.notify(sessionId, notificationId, notification)
	}

	private fun shadowNotificationManager() = shadowOf(
		assertNotNull(context.getSystemService<NotificationManager>())
	)

	private fun createFactory() = UninstallSessionFactoryImpl(
		applicationContext = context,
		defaultPackageInstallerService = lazy { RecordingPackageInstallerService() },
		ackpineServiceProviders = AckpineServiceProviders(lazy { emptySet() }),
		sessionDao = database.sessionDao(),
		sessionFailureDao = database.uninstallSessionDao(),
		executor = ImmediateExecutor,
		handler = Handler(Looper.getMainLooper())
	)
}