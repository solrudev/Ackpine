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
import ru.solrudev.ackpine.helpers.SerialExecutor
import ru.solrudev.ackpine.impl.database.dao.NativeSessionIdDao
import ru.solrudev.ackpine.impl.database.dao.SessionDao
import ru.solrudev.ackpine.impl.database.dao.SessionFailureDao
import ru.solrudev.ackpine.impl.database.dao.SessionProgressDao
import ru.solrudev.ackpine.impl.installer.session.IntentBasedInstallSession
import ru.solrudev.ackpine.impl.installer.session.SessionBasedInstallSession
import ru.solrudev.ackpine.installer.InstallFailure
import ru.solrudev.ackpine.installer.parameters.InstallParameters
import ru.solrudev.ackpine.installer.parameters.InstallerType
import ru.solrudev.ackpine.session.Progress
import ru.solrudev.ackpine.session.ProgressSession
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.session.parameters.NotificationData
import ru.solrudev.ackpine.session.parameters.NotificationString
import java.util.UUID
import java.util.concurrent.Executor

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal interface InstallSessionFactory {

	fun create(
		parameters: InstallParameters,
		id: UUID,
		initialState: Session.State<InstallFailure>,
		initialProgress: Progress,
		notificationId: Int
	): ProgressSession<InstallFailure>
}

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class InstallSessionFactoryImpl internal constructor(
	private val applicationContext: Context,
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
		notificationId: Int
	): ProgressSession<InstallFailure> = when (parameters.installerType) {
		InstallerType.INTENT_BASED -> IntentBasedInstallSession(
			applicationContext,
			apk = parameters.apks.toList().singleOrNull() ?: throw SplitPackagesNotSupportedException(),
			id, initialState, initialProgress,
			parameters.confirmation,
			parameters.notificationData.resolveDefault(parameters.name),
			sessionDao, sessionFailureDao, sessionProgressDao,
			executor, handler, notificationId
		)

		InstallerType.SESSION_BASED -> SessionBasedInstallSession(
			applicationContext,
			apks = parameters.apks.toList(),
			id, initialState, initialProgress,
			parameters.confirmation,
			parameters.notificationData.resolveDefault(parameters.name),
			parameters.requireUserAction,
			sessionDao, sessionFailureDao, sessionProgressDao, nativeSessionIdDao,
			executor, SerialExecutor(executor), handler, notificationId
		)
	}

	private fun NotificationData.resolveDefault(name: String): NotificationData = NotificationData.Builder()
		.setTitle(
			title.takeUnless { it.isDefault } ?: NotificationString.resource(R.string.ackpine_prompt_install_title)
		)
		.setContentText(
			contentText.takeUnless { it.isDefault } ?: resolveDefaultContentText(name)
		)
		.setIcon(icon)
		.build()

	private fun resolveDefaultContentText(name: String): NotificationString {
		if (name.isNotEmpty()) {
			return NotificationString.resource(R.string.ackpine_prompt_install_message_with_label, name)
		}
		return NotificationString.resource(R.string.ackpine_prompt_install_message)
	}
}