/*
 * Copyright (C) 2025 Ilya Fomichev
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

package ru.solrudev.ackpine.impl.helpers

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.os.Build
import androidx.annotation.RequiresApi
import ru.solrudev.ackpine.impl.receiver.SystemPackageInstallerStatusReceiver
import ru.solrudev.ackpine.session.parameters.Confirmation
import ru.solrudev.ackpine.session.parameters.NotificationData
import java.util.UUID

@JvmSynthetic
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
internal inline fun <reified T : SystemPackageInstallerStatusReceiver<*>> createPackageInstallerStatusIntentSender(
	context: Context,
	action: String,
	sessionId: UUID,
	confirmation: Confirmation,
	notificationId: Int,
	notificationData: NotificationData,
	requestCode: Int,
	putExtra: (Intent) -> Unit = {}
): IntentSender {
	val receiverIntent = Intent(context, T::class.java).apply {
		this.action = action
		putExtra(SystemPackageInstallerStatusReceiver.EXTRA_CONFIRMATION, confirmation.ordinal)
		putExtra(this)
		addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
	}
	SessionIdIntents.putSessionId(receiverIntent, sessionId)
	NotificationIntents.putNotification(receiverIntent, notificationData, notificationId)
	val receiverPendingIntent = PendingIntent.getBroadcast(
		context,
		requestCode,
		receiverIntent,
		UPDATE_CURRENT_FLAGS
	)
	return receiverPendingIntent.intentSender
}