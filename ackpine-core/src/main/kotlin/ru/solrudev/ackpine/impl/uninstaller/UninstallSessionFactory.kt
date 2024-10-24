/*
 * Copyright (C) 2023-2024 Ilya Fomichev
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

import android.content.Context
import android.os.Handler
import androidx.annotation.RestrictTo
import ru.solrudev.ackpine.core.R
import ru.solrudev.ackpine.helpers.concurrent.BinarySemaphore
import ru.solrudev.ackpine.impl.database.dao.SessionDao
import ru.solrudev.ackpine.impl.database.dao.SessionFailureDao
import ru.solrudev.ackpine.impl.uninstaller.helpers.getApplicationLabel
import ru.solrudev.ackpine.impl.uninstaller.session.UninstallSession
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.session.parameters.DEFAULT_NOTIFICATION_STRING
import ru.solrudev.ackpine.session.parameters.NotificationData
import ru.solrudev.ackpine.session.parameters.ResolvableString
import ru.solrudev.ackpine.uninstaller.UninstallFailure
import ru.solrudev.ackpine.uninstaller.parameters.UninstallParameters
import java.util.UUID
import java.util.concurrent.Executor

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal interface UninstallSessionFactory {

	fun create(
		parameters: UninstallParameters,
		id: UUID,
		initialState: Session.State<UninstallFailure>,
		notificationId: Int,
		dbWriteSemaphore: BinarySemaphore
	): Session<UninstallFailure>
}

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class UninstallSessionFactoryImpl internal constructor(
	private val applicationContext: Context,
	private val sessionDao: SessionDao,
	private val sessionFailureDao: SessionFailureDao<UninstallFailure>,
	private val executor: Executor,
	private val handler: Handler
) : UninstallSessionFactory {

	override fun create(
		parameters: UninstallParameters,
		id: UUID,
		initialState: Session.State<UninstallFailure>,
		notificationId: Int,
		dbWriteSemaphore: BinarySemaphore
	): Session<UninstallFailure> {
		return UninstallSession(
			applicationContext,
			parameters.packageName,
			id, initialState,
			parameters.confirmation,
			parameters.notificationData.resolveDefault(parameters.packageName),
			sessionDao, sessionFailureDao,
			executor, handler, notificationId, dbWriteSemaphore
		)
	}

	private fun NotificationData.resolveDefault(packageName: String): NotificationData = NotificationData.Builder()
		.setTitle(
			title.takeUnless { it === DEFAULT_NOTIFICATION_STRING } ?: AckpinePromptUninstallTitle
		)
		.setContentText(
			contentText.takeUnless { it === DEFAULT_NOTIFICATION_STRING } ?: resolveDefaultContentText(packageName)
		)
		.setIcon(icon)
		.build()

	private fun resolveDefaultContentText(packageName: String): ResolvableString {
		val label = applicationContext.packageManager.getApplicationLabel(packageName)?.toString()
		if (label != null) {
			return AckpinePromptUninstallMessageWithLabel(label)
		}
		return AckpinePromptUninstallMessage
	}
}

private object AckpinePromptUninstallTitle : ResolvableString.Resource(R.string.ackpine_prompt_uninstall_title) {
	private const val serialVersionUID = -4086992997791586590L
}

private object AckpinePromptUninstallMessage : ResolvableString.Resource(R.string.ackpine_prompt_uninstall_message) {
	private const val serialVersionUID = -3150252606151986307L
}

private class AckpinePromptUninstallMessageWithLabel(label: String) :
	ResolvableString.Resource(R.string.ackpine_prompt_uninstall_message_with_label, label) {

	private companion object {
		private const val serialVersionUID = 5259262335605612228L
	}
}