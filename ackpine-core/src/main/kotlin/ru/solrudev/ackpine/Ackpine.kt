/*
 * Copyright (C) 2023 Ilya Fomichev
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

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentCallbacks
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.core.content.getSystemService
import ru.solrudev.ackpine.core.R
import ru.solrudev.ackpine.exceptions.AckpineReinitializeException

/**
 * A library providing consistent APIs for installing and uninstalling apps on an Android device.
 */
public object Ackpine {

	@Volatile
	private var applicationContext: Context? = null

	private val lock = Any()

	private val configurationChangesCallback = object : ComponentCallbacks {
		override fun onConfigurationChanged(newConfig: Configuration) = createNotificationChannel()
		override fun onLowMemory() {}
	}

	@JvmSynthetic
	internal fun init(context: Context) {
		if (applicationContext != null) {
			throw AckpineReinitializeException()
		}
		synchronized(lock) {
			applicationContext = context.applicationContext.also {
				it.registerComponentCallbacks(configurationChangesCallback)
			}
			createNotificationChannel()
		}
	}

	private fun createNotificationChannel() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
			return
		}
		val applicationContext = applicationContext ?: return
		val channelIdString = applicationContext.getString(R.string.ackpine_notification_channel_id)
		val channelName = applicationContext.getString(R.string.ackpine_notification_channel_name)
		val channelDescription = applicationContext.getString(R.string.ackpine_notification_channel_description)
		val importance = NotificationManager.IMPORTANCE_HIGH
		val channel = NotificationChannel(channelIdString, channelName, importance).apply {
			description = channelDescription
		}
		applicationContext.getSystemService<NotificationManager>()?.createNotificationChannel(channel)
	}
}