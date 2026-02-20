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

import androidx.core.net.toUri
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import ru.solrudev.ackpine.impl.database.dao.SessionProgressDao
import ru.solrudev.ackpine.impl.database.model.InstallConstraintsEntity
import ru.solrudev.ackpine.impl.database.model.InstallModeEntity
import ru.solrudev.ackpine.impl.database.model.InstallPreapprovalEntity
import ru.solrudev.ackpine.impl.database.model.SessionEntity
import ru.solrudev.ackpine.impl.testutil.createInstallSessionEntity
import ru.solrudev.ackpine.installer.parameters.InstallConstraints
import ru.solrudev.ackpine.installer.parameters.InstallConstraints.TimeoutStrategy
import ru.solrudev.ackpine.installer.parameters.InstallMode
import ru.solrudev.ackpine.installer.parameters.InstallPreapproval
import ru.solrudev.ackpine.installer.parameters.InstallerType
import ru.solrudev.ackpine.session.Progress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

@RunWith(RobolectricTestRunner::class)
class InstallSessionMappersTest {

	@Test
	fun getProgressReturnsProgressFromDao() {
		val progressDao = TestSessionProgressDao(Progress(50, 100))
		val session = createInstallSession()
		val progress = session.getProgress(progressDao)
		assertEquals(Progress(50, 100), progress)
	}

	@Test
	fun getProgressReturnsDefaultWhenAbsent() {
		val progressDao = TestSessionProgressDao(null)
		val session = createInstallSession()
		val progress = session.getProgress(progressDao)
		assertEquals(Progress(), progress)
	}

	@Test
	fun getInstallModeReturnsFullWhenNull() {
		val session = createInstallSession(installMode = null)
		val mode = session.getInstallMode()
		assertEquals(InstallMode.Full, mode)
	}

	@Test
	fun getInstallModeReturnsFull() {
		val session = createInstallSession(
			installMode = InstallModeEntity("session-id", InstallModeEntity.InstallMode.FULL, false)
		)
		val mode = session.getInstallMode()
		assertEquals(InstallMode.Full, mode)
	}

	@Test
	fun getInstallModeReturnsInheritExisting() {
		val session = createInstallSession(
			installMode = InstallModeEntity("session-id", InstallModeEntity.InstallMode.INHERIT_EXISTING, true),
			packageName = "com.example.app"
		)
		val mode = session.getInstallMode()
		assertEquals(InstallMode.InheritExisting("com.example.app", dontKillApp = true), mode)
	}

	@Test
	fun getPreapprovalReturnsNoneWhenNull() {
		val session = createInstallSession(preapproval = null)
		val preapproval = session.getPreapproval()
		assertEquals(InstallPreapproval.NONE, preapproval)
	}

	@Test
	fun getPreapprovalReturnsPreapprovalFromEntity() {
		val entity = InstallPreapprovalEntity(
			sessionId = "session-id",
			packageName = "com.example.app",
			label = "Example App",
			locale = "en-US",
			icon = "content://example/icon.png",
			fallbackToOnDemandApproval = true
		)
		val session = createInstallSession(preapproval = entity)

		val expectedPreapproval = InstallPreapproval("com.example.app", "Example App", "en-US") {
			icon = "content://example/icon.png".toUri()
			fallbackToOnDemandApproval = true
		}
		val preapproval = session.getPreapproval()

		assertEquals(expectedPreapproval, preapproval)
	}

	@Test
	fun getConstraintsReturnsNoneWhenNull() {
		val session = createInstallSession(constraints = null)
		val constraints = session.getConstraints()
		assertEquals(InstallConstraints.NONE, constraints)
	}

	@Test
	fun getConstraintsReturnsConstraintsFromEntity() {
		val entity = InstallConstraintsEntity(
			sessionId = "session-id",
			isAppNotForegroundRequired = true,
			isAppNotInteractingRequired = true,
			isAppNotTopVisibleRequired = false,
			isDeviceIdleRequired = true,
			isNotInCallRequired = false,
			timeoutMillis = 5000L,
			timeoutStrategy = TimeoutStrategy.Retry(3)
		)
		val session = createInstallSession(constraints = entity)

		val expectedConstraints = InstallConstraints(5.seconds) {
			isAppNotForegroundRequired = true
			isAppNotInteractingRequired = true
			isAppNotTopVisibleRequired = false
			isDeviceIdleRequired = true
			isNotInCallRequired = false
			timeoutStrategy = TimeoutStrategy.Retry(3)
		}
		val constraints = session.getConstraints()

		assertEquals(expectedConstraints, constraints)
	}

	@Test
	fun installModeFullToEntity() {
		val mode = InstallMode.Full
		val expectedEntity = InstallModeEntity(
			sessionId = "session-123",
			installMode = InstallModeEntity.InstallMode.FULL,
			dontKillApp = false
		)
		val entity = mode.toEntity("session-123")
		assertEquals(expectedEntity, entity)
	}

	@Test
	fun installModeInheritExistingToEntity() {
		val mode = InstallMode.InheritExisting("com.example.app", dontKillApp = true)
		val expectedEntity = InstallModeEntity(
			sessionId = "session-123",
			installMode = InstallModeEntity.InstallMode.INHERIT_EXISTING,
			dontKillApp = true
		)
		val entity = mode.toEntity("session-123")
		assertEquals(expectedEntity, entity)
	}

	@Test
	fun installPreapprovalToEntity() {
		val preapproval = InstallPreapproval("com.example.app", "Example", "en-US") {
			icon = "content://icon".toUri()
			fallbackToOnDemandApproval = true
		}
		val expectedEntity = InstallPreapprovalEntity(
			sessionId = "session-123",
			packageName = "com.example.app",
			label = "Example",
			locale = "en-US",
			icon = "content://icon",
			fallbackToOnDemandApproval = true
		)
		val entity = preapproval.toEntity("session-123")
		assertEquals(expectedEntity, entity)
	}

	@Test
	fun installConstraintsToEntity() {
		val constraints = InstallConstraints(5.seconds) {
			isAppNotForegroundRequired = true
			isDeviceIdleRequired = true
			timeoutStrategy = TimeoutStrategy.CommitEagerly
		}

		val expectedEntity = InstallConstraintsEntity(
			sessionId = "session-123",
			isAppNotForegroundRequired = true,
			isAppNotInteractingRequired = false,
			isAppNotTopVisibleRequired = false,
			isDeviceIdleRequired = true,
			isNotInCallRequired = false,
			timeoutMillis = 5000L,
			timeoutStrategy = TimeoutStrategy.CommitEagerly
		)
		val entity = constraints.toEntity("session-123")

		assertEquals(expectedEntity, entity)
	}

	private fun createInstallSession(
		installMode: InstallModeEntity? = null,
		preapproval: InstallPreapprovalEntity? = null,
		constraints: InstallConstraintsEntity? = null,
		packageName: String? = null
	): SessionEntity.InstallSession = createInstallSessionEntity(
		id = "session-id",
		state = SessionEntity.State.PENDING,
		installerType = InstallerType.SESSION_BASED,
		uris = listOf("file:///test.apk"),
		installMode = installMode,
		preapproval = preapproval,
		constraints = constraints,
		packageName = packageName
	)

	private class TestSessionProgressDao(private val progressToReturn: Progress?) : SessionProgressDao {
		override fun getProgress(id: String) = progressToReturn
		override fun initProgress(id: String) {}
		override fun updateProgress(id: String, progress: Int, max: Int) {}
	}
}