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

@file:Suppress("ConstPropertyName", "Unused")

package ru.solrudev.ackpine.impl.uninstaller

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import androidx.annotation.RestrictTo
import androidx.annotation.WorkerThread
import ru.solrudev.ackpine.core.R
import ru.solrudev.ackpine.impl.database.dao.SessionDao
import ru.solrudev.ackpine.impl.database.dao.SessionFailureDao
import ru.solrudev.ackpine.impl.database.getNotificationData
import ru.solrudev.ackpine.impl.database.getPlugins
import ru.solrudev.ackpine.impl.database.getState
import ru.solrudev.ackpine.impl.database.model.SessionEntity
import ru.solrudev.ackpine.impl.helpers.concurrent.BinarySemaphore
import ru.solrudev.ackpine.impl.plugability.AckpineServiceProviders
import ru.solrudev.ackpine.impl.services.PackageInstallerService
import ru.solrudev.ackpine.impl.session.CompletableSession
import ru.solrudev.ackpine.impl.uninstaller.helpers.getApplicationLabel
import ru.solrudev.ackpine.impl.uninstaller.session.IntentBasedUninstallSession
import ru.solrudev.ackpine.impl.uninstaller.session.PackageInstallerBasedUninstallSession
import ru.solrudev.ackpine.resources.ResolvableString
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.session.parameters.DEFAULT_NOTIFICATION_STRING
import ru.solrudev.ackpine.session.parameters.NotificationData
import ru.solrudev.ackpine.uninstaller.UninstallFailure
import ru.solrudev.ackpine.uninstaller.parameters.UninstallParameters
import ru.solrudev.ackpine.uninstaller.parameters.UninstallerType
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
	): CompletableSession<UninstallFailure>

	@WorkerThread
	fun create(uninstallSession: SessionEntity.UninstallSession): CompletableSession<UninstallFailure>

	fun resolveNotificationData(notificationData: NotificationData, packageName: String): NotificationData
}

@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressLint("NewApi")
internal class UninstallSessionFactoryImpl internal constructor(
	private val applicationContext: Context,
	private val defaultPackageInstallerService: Lazy<PackageInstallerService>,
	private val ackpineServiceProviders: AckpineServiceProviders,
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
	) = when (parameters.uninstallerType) {
		UninstallerType.INTENT_BASED -> IntentBasedUninstallSession(
			applicationContext,
			parameters.packageName,
			id, initialState,
			parameters.confirmation,
			resolveNotificationData(parameters.notificationData, parameters.packageName),
			sessionDao, sessionFailureDao,
			executor, handler, notificationId, dbWriteSemaphore
		)

		UninstallerType.PACKAGE_INSTALLER_BASED -> {
			val plugins = runCatching { parameters.pluginContainer.getPlugins() }
			ackpineServiceProviders.createSessionWithService(
				serviceClass = PackageInstallerService::class,
				defaultService = defaultPackageInstallerService,
				sessionId = id,
				pluginClasses = plugins.map { it.keys },
				pluginParameters = plugins.map { it.values }
			) { packageInstallerService ->
				PackageInstallerBasedUninstallSession(
					applicationContext,
					packageInstallerService,
					parameters.packageName,
					id, initialState,
					parameters.confirmation,
					resolveNotificationData(parameters.notificationData, parameters.packageName),
					sessionDao, sessionFailureDao,
					executor, handler, notificationId, dbWriteSemaphore
				)
			}
		}
	}

	override fun create(uninstallSession: SessionEntity.UninstallSession): CompletableSession<UninstallFailure> {
		val sessionId = UUID.fromString(uninstallSession.session.id)
		val packageName = uninstallSession.packageName
		val initialState = uninstallSession.getState(sessionFailureDao)
		val confirmation = uninstallSession.session.confirmation
		val notificationData = uninstallSession.getNotificationData()
		val notificationId = uninstallSession.notificationId!!
		val session = when (uninstallSession.uninstallerType) {
			UninstallerType.INTENT_BASED -> IntentBasedUninstallSession(
				applicationContext,
				packageName,
				sessionId, initialState,
				confirmation,
				resolveNotificationData(notificationData, packageName),
				sessionDao, sessionFailureDao,
				executor, handler, notificationId, BinarySemaphore()
			)

			UninstallerType.PACKAGE_INSTALLER_BASED -> {
				val plugins = runCatching { uninstallSession.getPlugins() }
				ackpineServiceProviders.createSessionWithService(
					serviceClass = PackageInstallerService::class,
					defaultService = defaultPackageInstallerService,
					sessionId,
					pluginClasses = plugins
				) { packageInstallerService ->
					PackageInstallerBasedUninstallSession(
						applicationContext,
						packageInstallerService,
						packageName,
						sessionId, initialState,
						confirmation,
						resolveNotificationData(notificationData, packageName),
						sessionDao, sessionFailureDao,
						executor, handler, notificationId, BinarySemaphore()
					)
				}
			}
		}
		if (initialState.isTerminal) {
			session.cleanup()
		}
		return session
	}

	override fun resolveNotificationData(
		notificationData: NotificationData,
		packageName: String
	) = notificationData.run {
		NotificationData.Builder()
			.setTitle(
				title.takeUnless { it === DEFAULT_NOTIFICATION_STRING } ?: AckpinePromptUninstallTitle
			)
			.setContentText(
				contentText.takeUnless { it === DEFAULT_NOTIFICATION_STRING } ?: resolveDefaultContentText(packageName)
			)
			.setIcon(icon)
			.build()
	}

	private fun resolveDefaultContentText(packageName: String): ResolvableString {
		val label = applicationContext.packageManager.getApplicationLabel(packageName)?.toString()
		if (label != null) {
			return AckpinePromptUninstallMessageWithLabel(label)
		}
		return AckpinePromptUninstallMessage
	}
}

private object AckpinePromptUninstallTitle : ResolvableString.Resource() {
	private const val serialVersionUID = -4086992997791586590L
	override fun stringId() = R.string.ackpine_prompt_uninstall_title
	private fun readResolve(): Any = AckpinePromptUninstallTitle
}

private object AckpinePromptUninstallMessage : ResolvableString.Resource() {
	private const val serialVersionUID = -3150252606151986307L
	override fun stringId(): Int = R.string.ackpine_prompt_uninstall_message
	private fun readResolve(): Any = AckpinePromptUninstallMessage
}

private class AckpinePromptUninstallMessageWithLabel(label: String) : ResolvableString.Resource(label) {

	override fun stringId() = R.string.ackpine_prompt_uninstall_message_with_label

	private companion object {
		private const val serialVersionUID = 5259262335605612228L
	}
}