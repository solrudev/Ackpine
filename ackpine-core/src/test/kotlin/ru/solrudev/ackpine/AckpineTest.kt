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

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.test.core.app.ApplicationProvider
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import ru.solrudev.ackpine.core.R
import ru.solrudev.ackpine.exceptions.AckpineReinitializeException
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class AckpineTest {

	private val context: Context = ApplicationProvider.getApplicationContext()

	@AfterTest
	fun tearDown() {
		Ackpine.reset()
	}

	@Test
	fun initCreatesNotificationChannel() {
		Ackpine.init(context)
		val channelId = context.getString(R.string.ackpine_notification_channel_id)
		val notificationManager = NotificationManagerCompat.from(context)
		val channel = notificationManager.getNotificationChannel(channelId)
		assertNotNull(channel)
		assertEquals(NotificationManager.IMPORTANCE_HIGH, channel.importance)
	}

	@Test
	fun initThrowsOnSecondCall() {
		Ackpine.init(context)
		assertFailsWith<AckpineReinitializeException> {
			Ackpine.init(context)
		}
	}

	@Test
	fun deleteNotificationChannelRemovesChannel() {
		Ackpine.init(context)
		val channelId = context.getString(R.string.ackpine_notification_channel_id)
		val notificationManager = NotificationManagerCompat.from(context)
		Ackpine.deleteNotificationChannel(context)
		val channel = notificationManager.getNotificationChannel(channelId)
		assertNull(channel)
	}
}