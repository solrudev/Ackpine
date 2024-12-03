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

package ru.solrudev.ackpine.impl.installer

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import androidx.annotation.RestrictTo
import ru.solrudev.ackpine.core.R
import ru.solrudev.ackpine.exceptions.SplitPackagesNotSupportedException
import ru.solrudev.ackpine.helpers.concurrent.BinarySemaphore
import ru.solrudev.ackpine.impl.database.dao.LastUpdateTimestampDao
import ru.solrudev.ackpine.impl.database.dao.NativeSessionIdDao
import ru.solrudev.ackpine.impl.database.dao.SessionDao
import ru.solrudev.ackpine.impl.database.dao.SessionFailureDao
import ru.solrudev.ackpine.impl.database.dao.SessionProgressDao
import ru.solrudev.ackpine.impl.installer.session.IntentBasedInstallSession
import ru.solrudev.ackpine.impl.installer.session.SessionBasedInstallSession
import ru.solrudev.ackpine.installer.InstallFailure
import ru.solrudev.ackpine.installer.parameters.InstallParameters
import ru.solrudev.ackpine.installer.parameters.InstallerType
import ru.solrudev.ackpine.resources.ResolvableString
import ru.solrudev.ackpine.session.Progress
import ru.solrudev.ackpine.session.ProgressSession
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.session.parameters.DEFAULT_NOTIFICATION_STRING
import ru.solrudev.ackpine.session.parameters.NotificationData
import java.util.UUID
import java.util.concurrent.Executor

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal interface InstallSessionFactory {

	fun create(
		parameters: InstallParameters,
		id: UUID,
		initialState: Session.State<InstallFailure>,
		initialProgress: Progress,
		notificationId: Int,
		dbWriteSemaphore: BinarySemaphore,
		packageName: String = "",
		lastUpdateTimestamp: Long = Long.MAX_VALUE,
		needToCompleteIfSucceeded: Boolean = false
	): ProgressSession<InstallFailure>

	fun resolveNotificationData(notificationData: NotificationData, name: String): NotificationData
}

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class InstallSessionFactoryImpl internal constructor(
	private val applicationContext: Context,
	private val lastUpdateTimestampDao: LastUpdateTimestampDao,
	private val sessionDao: SessionDao,
	private val sessionFailureDao: SessionFailureDao<InstallFailure>,
	private val sessionProgressDao: SessionProgressDao,
	private val nativeSessionIdDao: NativeSessionIdDao,
	private val executor: Executor,
	private val handler: Handler
) : InstallSessionFactory {

	@SuppressLint("NewApi")
	override fun create(
		parameters: InstallParameters,
		id: UUID,
		initialState: Session.State<InstallFailure>,
		initialProgress: Progress,
		notificationId: Int,
		dbWriteSemaphore: BinarySemaphore,
		packageName: String,
		lastUpdateTimestamp: Long,
		needToCompleteIfSucceeded: Boolean
	): ProgressSession<InstallFailure> = when (parameters.installerType) {
		InstallerType.INTENT_BASED -> IntentBasedInstallSession(
			applicationContext,
			apk = parameters.apks.toList().singleOrNull() ?: throw SplitPackagesNotSupportedException(),
			id, initialState, initialProgress,
			parameters.confirmation,
			resolveNotificationData(parameters.notificationData, parameters.name),
			lastUpdateTimestampDao, sessionDao, sessionFailureDao, sessionProgressDao,
			executor, handler,
			notificationId, packageName, lastUpdateTimestamp, needToCompleteIfSucceeded,
			dbWriteSemaphore
		)

		InstallerType.SESSION_BASED -> SessionBasedInstallSession(
			applicationContext,
			apks = parameters.apks.toList(),
			id, initialState, initialProgress,
			parameters.confirmation,
			resolveNotificationData(parameters.notificationData, parameters.name),
			parameters.requireUserAction,
			parameters.installMode, parameters.constraints,
			parameters.requestUpdateOwnership, parameters.packageSource,
			sessionDao, sessionFailureDao, sessionProgressDao, nativeSessionIdDao,
			executor, handler, notificationId, dbWriteSemaphore
		)
	}

	override fun resolveNotificationData(notificationData: NotificationData, name: String) = notificationData.run {
		NotificationData.Builder()
			.setTitle(
				title.takeUnless { it === DEFAULT_NOTIFICATION_STRING } ?: AckpinePromptInstallTitle
			)
			.setContentText(
				contentText.takeUnless { it === DEFAULT_NOTIFICATION_STRING } ?: resolveDefaultContentText(name)
			)
			.setIcon(icon)
			.build()
	}

	private fun resolveDefaultContentText(name: String): ResolvableString {
		if (name.isNotEmpty()) {
			return AckpinePromptInstallMessageWithLabel(name)
		}
		return AckpinePromptInstallMessage
	}
}

private object AckpinePromptInstallTitle : ResolvableString.Resource() {
	private const val serialVersionUID = 7815666924791958742L
	override fun stringId() = R.string.ackpine_prompt_install_title
	private fun readResolve(): Any = AckpinePromptInstallTitle
}

private object AckpinePromptInstallMessage : ResolvableString.Resource() {
	private const val serialVersionUID = 1224637050663404482L
	override fun stringId() = R.string.ackpine_prompt_install_message
	private fun readResolve(): Any = AckpinePromptInstallMessage
}

private class AckpinePromptInstallMessageWithLabel(name: String) : ResolvableString.Resource(name) {

	override fun stringId() = R.string.ackpine_prompt_install_message_with_label

	private companion object {
		private const val serialVersionUID = -6931607904159775056L
	}
}