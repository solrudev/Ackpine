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

package ru.solrudev.ackpine.impl.database

import android.content.Context
import android.os.Build
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.solrudev.ackpine.impl.database.model.SessionEntity
import ru.solrudev.ackpine.impl.testutil.TestDrawableId
import ru.solrudev.ackpine.resources.ResolvableString
import ru.solrudev.ackpine.session.parameters.Confirmation
import java.io.File
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class PurgeCallbackTest {

	private val context: Context = ApplicationProvider.getApplicationContext()
	private val databaseFile by lazy { File(context.cacheDir, "purge_tests.db") }
	private lateinit var database: AckpineDatabase

	@BeforeTest
	fun setUp() {
		databaseFile.delete()
		database = createDatabase()
	}

	@AfterTest
	fun tearDown() {
		database.close()
		databaseFile.delete()
	}

	private fun createDatabase(): AckpineDatabase = Room
		.databaseBuilder(context, AckpineDatabase::class.java, databaseFile.absolutePath)
		.addCallback(PurgeCallback)
		.allowMainThreadQueries()
		.build()

	@Test
	fun onOpenPurgesTerminalSessionsOlderThanOneDay() {
		val oldTimestamp = System.currentTimeMillis() - 2.days.inWholeMilliseconds
		val oldSucceededId = insertSession(
			state = SessionEntity.State.SUCCEEDED,
			lastLaunchTimestamp = oldTimestamp
		)
		val oldFailedId = insertSession(
			state = SessionEntity.State.FAILED,
			lastLaunchTimestamp = oldTimestamp
		)
		val oldCancelledId = insertSession(
			state = SessionEntity.State.CANCELLED,
			lastLaunchTimestamp = oldTimestamp
		)

		reopenDatabase()

		assertFalse(sessionExists(oldSucceededId))
		assertFalse(sessionExists(oldFailedId))
		assertFalse(sessionExists(oldCancelledId))
	}

	@Test
	fun onOpenPreservesTerminalSessionsNewerThanOneDay() {
		val recentTimestamp = System.currentTimeMillis() - 12.hours.inWholeMilliseconds
		val recentSucceededId = insertSession(
			state = SessionEntity.State.SUCCEEDED,
			lastLaunchTimestamp = recentTimestamp
		)
		val recentFailedId = insertSession(
			state = SessionEntity.State.FAILED,
			lastLaunchTimestamp = recentTimestamp
		)
		val recentCancelledId = insertSession(
			state = SessionEntity.State.CANCELLED,
			lastLaunchTimestamp = recentTimestamp
		)

		reopenDatabase()

		assertTrue(sessionExists(recentSucceededId))
		assertTrue(sessionExists(recentFailedId))
		assertTrue(sessionExists(recentCancelledId))
	}

	@Test
	fun onOpenPreservesNonTerminalSessions() {
		val oldTimestamp = System.currentTimeMillis() - 2.days.inWholeMilliseconds
		val oldPendingId = insertSession(
			state = SessionEntity.State.PENDING,
			lastLaunchTimestamp = oldTimestamp
		)
		val oldActiveId = insertSession(
			state = SessionEntity.State.ACTIVE,
			lastLaunchTimestamp = oldTimestamp
		)
		val oldAwaitingId = insertSession(
			state = SessionEntity.State.AWAITING,
			lastLaunchTimestamp = oldTimestamp
		)
		val oldCommittedId = insertSession(
			state = SessionEntity.State.COMMITTED,
			lastLaunchTimestamp = oldTimestamp
		)

		reopenDatabase()

		assertTrue(sessionExists(oldPendingId))
		assertTrue(sessionExists(oldActiveId))
		assertTrue(sessionExists(oldAwaitingId))
		assertTrue(sessionExists(oldCommittedId))
	}

	@Test
	fun onOpenPurgesOnlyOldTerminalSessions() {
		val oldTimestamp = System.currentTimeMillis() - 2.days.inWholeMilliseconds
		val recentTimestamp = System.currentTimeMillis() - 6.hours.inWholeMilliseconds
		val oldSucceededId = insertSession(
			state = SessionEntity.State.SUCCEEDED,
			lastLaunchTimestamp = oldTimestamp
		)
		val oldFailedId = insertSession(
			state = SessionEntity.State.FAILED,
			lastLaunchTimestamp = oldTimestamp
		)
		val recentSucceededId = insertSession(
			state = SessionEntity.State.SUCCEEDED,
			lastLaunchTimestamp = recentTimestamp
		)
		val oldPendingId = insertSession(
			state = SessionEntity.State.PENDING,
			lastLaunchTimestamp = oldTimestamp
		)
		val oldActiveId = insertSession(
			state = SessionEntity.State.ACTIVE,
			lastLaunchTimestamp = oldTimestamp
		)

		reopenDatabase()

		assertFalse(sessionExists(oldSucceededId))
		assertFalse(sessionExists(oldFailedId))
		assertTrue(sessionExists(recentSucceededId))
		assertTrue(sessionExists(oldPendingId))
		assertTrue(sessionExists(oldActiveId))
		assertEquals(3, countSessions())
	}

	private fun insertSession(
		state: SessionEntity.State,
		lastLaunchTimestamp: Long
	): String {
		val id = UUID.randomUUID().toString()
		val session = SessionEntity(
			id = id,
			type = SessionEntity.Type.INSTALL,
			state = state,
			confirmation = Confirmation.DEFERRED,
			notificationTitle = ResolvableString.empty(),
			notificationText = ResolvableString.empty(),
			notificationIcon = TestDrawableId,
			requireUserAction = true,
			lastLaunchTimestamp = lastLaunchTimestamp,
			lastCommitTimestamp = 0
		)
		database.sessionDao().insertSession(session)
		return id
	}

	private fun sessionExists(id: String): Boolean {
		database.openHelper.readableDatabase.query("SELECT 1 FROM sessions WHERE id = ?", arrayOf(id)).use { cursor ->
			return cursor.count > 0
		}
	}

	private fun countSessions(): Int {
		database.openHelper.readableDatabase.query("SELECT COUNT(*) FROM sessions").use { cursor ->
			cursor.moveToFirst()
			return cursor.getInt(0)
		}
	}

	private fun reopenDatabase() {
		database.close()
		database = createDatabase()
	}
}