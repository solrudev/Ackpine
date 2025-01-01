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

import android.app.NotificationManager
import android.content.ComponentCallbacks
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import ru.solrudev.ackpine.core.R
import ru.solrudev.ackpine.exceptions.AckpineReinitializeException
import ru.solrudev.ackpine.session.parameters.Confirmation.DEFERRED

/**
 * A library providing consistent APIs for installing and uninstalling apps on an Android device.
 */
public object Ackpine {

	@Volatile
	private var applicationContext: Context? = null

	private val lock = Any()

	private val configurationChangesCallback = object : ComponentCallbacks {
		override fun onConfigurationChanged(newConfig: Configuration) = createNotificationChannel()
		override fun onLowMemory() { /* no-op */ }
	}

	/**
	 * Deletes previously created Ackpine's notification channel from application's notification settings.
	 *
	 * Use this **only** in conjunction with disabling automatic Ackpine initialization and if [DEFERRED] confirmation
	 * is never used in the app.
	 *
	 * To disable Ackpine initializer, add the following lines to the app's `AndroidManifest.xml`:
	 *
	 * ```xml
	 * <provider
	 *     android:name="androidx.startup.InitializationProvider"
	 *     android:authorities="${applicationId}.androidx-startup"
	 *     android:exported="false"
	 *     tools:node="merge">
	 *     <meta-data
	 *         android:name="ru.solrudev.ackpine.AckpineInitializer"
	 *         tools:node="remove" />
	 * </provider>
	 * ```
	 */
	@JvmStatic
	public fun deleteNotificationChannel(context: Context) {
		val applicationContext = context.applicationContext
		applicationContext.unregisterComponentCallbacks(configurationChangesCallback)
		val channelId = applicationContext.getString(R.string.ackpine_notification_channel_id)
		NotificationManagerCompat.from(applicationContext).deleteNotificationChannel(channelId)
	}

	@JvmSynthetic
	internal fun init(context: Context) {
		if (applicationContext != null) {
			throw AckpineReinitializeException()
		}
		synchronized(lock) {
			applicationContext = context.applicationContext.also { ctx ->
				ctx.registerComponentCallbacks(configurationChangesCallback)
				createNotificationChannel(ctx)
			}
		}
	}

	private fun createNotificationChannel(context: Context? = applicationContext) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
			return
		}
		val applicationContext = context ?: return
		val channelId = applicationContext.getString(R.string.ackpine_notification_channel_id)
		val channelName = applicationContext.getString(R.string.ackpine_notification_channel_name)
		val channelDescription = applicationContext.getString(R.string.ackpine_notification_channel_description)
		val importance = NotificationManager.IMPORTANCE_HIGH
		val channel = NotificationChannelCompat.Builder(channelId, importance)
			.setName(channelName)
			.setDescription(channelDescription)
			.setVibrationEnabled(true)
			.setLightsEnabled(true)
			.setShowBadge(true)
			.build()
		NotificationManagerCompat.from(applicationContext).createNotificationChannel(channel)
	}
}