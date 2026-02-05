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

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.solrudev.ackpine.impl.helpers.concurrent.BinarySemaphore
import ru.solrudev.ackpine.impl.services.PackageInstallerService
import ru.solrudev.ackpine.impl.testutil.ImmediateExecutor
import ru.solrudev.ackpine.impl.testutil.RecordingPackageInstallerService
import ru.solrudev.ackpine.impl.testutil.RecordingSessionDao
import ru.solrudev.ackpine.impl.testutil.TestSessionFailureDao
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.session.parameters.Confirmation
import ru.solrudev.ackpine.session.parameters.NotificationData
import ru.solrudev.ackpine.uninstaller.UninstallFailure
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class PackageInstallerBasedUninstallSessionTest {

	private val context: Context = ApplicationProvider.getApplicationContext()

	@Test
	fun commitCallsPackageInstallerUninstall() {
		val packageInstaller = RecordingPackageInstallerService()
		val sessionId = UUID.randomUUID()
		val session = createSession(
			packageInstaller = packageInstaller,
			id = sessionId
		)

		assertTrue(session.commit())

		val call = packageInstaller.uninstallCalls.single()
		assertEquals("com.example.app", call.packageName)
		assertEquals(sessionId, call.ackpineSessionId)
	}

	private fun createSession(
		packageInstaller: PackageInstallerService,
		id: UUID,
		initialState: Session.State<UninstallFailure> = Session.State.Awaiting
	) = PackageInstallerBasedUninstallSession(
		context = context,
		packageInstaller = packageInstaller,
		packageName = "com.example.app",
		id = id,
		initialState = initialState,
		confirmation = Confirmation.DEFERRED,
		notificationData = NotificationData.DEFAULT,
		sessionDao = RecordingSessionDao(),
		sessionFailureDao = TestSessionFailureDao(),
		executor = ImmediateExecutor,
		handler = Handler(Looper.getMainLooper()),
		notificationId = 1,
		dbWriteSemaphore = BinarySemaphore()
	)
}