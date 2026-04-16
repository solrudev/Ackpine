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

package ru.solrudev.ackpine

import android.util.Log
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLog
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
class AckpineLoggerLogcatTest {

	private val logger = AckpineLogger.Logcat()

	@AfterTest
	fun tearDown() {
		ShadowLog.clear()
	}

	@Test
	fun levelMappingMatchesAndroidLogLevels() {
		logger.verbose("TagV", "verbose")
		logger.debug("TagD", "debug")
		logger.info("TagI", "info")
		logger.warn("TagW", "warn")
		logger.error("TagE", "error")

		val logs = listOf("TagV", "TagD", "TagI", "TagW", "TagE")
			.map { tag -> ShadowLog.getLogsForTag(tag).last() }
		assertEquals(listOf(Log.VERBOSE, Log.DEBUG, Log.INFO, Log.WARN, Log.ERROR), logs.map { it.type })
	}

	@Test
	fun formattingUsesLocaleRoot() {
		logger.info("Tag", "%.2f", 1.5)

		val log = ShadowLog.getLogsForTag("Tag").last()
		assertEquals("1.50", log.msg)
	}

	@Test
	fun throwableIsForwardedToLogcat() {
		val throwable = IllegalStateException("boom")

		logger.error("Tag", throwable, "message %s", "value")

		val log = ShadowLog.getLogsForTag("Tag").last()
		assertEquals("message value", log.msg)
		assertEquals(throwable, log.throwable)
	}

	@Test
	fun invalidFormatFallsBackWithoutThrowing() {
		logger.info("Tag", "%q", "value")

		val log = ShadowLog.getLogsForTag("Tag").last()
		assertContains(log.msg, "%q")
		assertContains(log.msg, "formatting failed")
		assertContains(log.msg, "[value]")
		assertNotNull(log)
	}
}