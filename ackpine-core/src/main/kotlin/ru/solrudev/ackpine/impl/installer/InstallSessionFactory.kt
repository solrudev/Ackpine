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

package ru.solrudev.ackpine.impl.installer

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Handler
import androidx.annotation.RestrictTo
import androidx.annotation.WorkerThread
import androidx.core.net.toUri
import ru.solrudev.ackpine.core.R
import ru.solrudev.ackpine.exceptions.SplitPackagesNotSupportedException
import ru.solrudev.ackpine.impl.database.dao.InstallConstraintsDao
import ru.solrudev.ackpine.impl.database.dao.InstallPreapprovalDao
import ru.solrudev.ackpine.impl.database.dao.InstallSessionDao
import ru.solrudev.ackpine.impl.database.dao.LastUpdateTimestampDao
import ru.solrudev.ackpine.impl.database.dao.NativeSessionIdDao
import ru.solrudev.ackpine.impl.database.dao.SessionDao
import ru.solrudev.ackpine.impl.database.dao.SessionProgressDao
import ru.solrudev.ackpine.impl.database.model.SessionEntity
import ru.solrudev.ackpine.impl.helpers.concurrent.BinarySemaphore
import ru.solrudev.ackpine.impl.installer.session.IntentBasedInstallSession
import ru.solrudev.ackpine.impl.installer.session.SessionBasedInstallSession
import ru.solrudev.ackpine.impl.installer.session.helpers.PROGRESS_MAX
import ru.solrudev.ackpine.impl.plugability.AckpineServiceProvider
import ru.solrudev.ackpine.impl.plugability.get
import ru.solrudev.ackpine.impl.session.CompletableProgressSession
import ru.solrudev.ackpine.installer.InstallFailure
import ru.solrudev.ackpine.installer.parameters.InstallParameters
import ru.solrudev.ackpine.installer.parameters.InstallerType
import ru.solrudev.ackpine.installer.parameters.PackageSource
import ru.solrudev.ackpine.plugability.AckpinePlugin
import ru.solrudev.ackpine.plugability.AckpinePluginCache
import ru.solrudev.ackpine.resources.ResolvableString
import ru.solrudev.ackpine.session.Progress
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.session.Session.State.Committed
import ru.solrudev.ackpine.session.Session.State.Succeeded
import ru.solrudev.ackpine.session.parameters.DEFAULT_NOTIFICATION_STRING
import ru.solrudev.ackpine.session.parameters.NotificationData
import java.util.UUID
import java.util.concurrent.Executor

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal interface InstallSessionFactory {

	fun create(
		parameters: InstallParameters,
		id: UUID,
		notificationId: Int,
		dbWriteSemaphore: BinarySemaphore
	): CompletableProgressSession<InstallFailure>

	@WorkerThread
	fun create(
		session: SessionEntity.InstallSession,
		completeIfSucceeded: Boolean = false
	): CompletableProgressSession<InstallFailure>

	fun resolveNotificationData(notificationData: NotificationData, name: String): NotificationData
}

@SuppressLint("NewApi")
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class InstallSessionFactoryImpl internal constructor(
	private val applicationContext: Context,
	private val defaultPackageInstallerService: Lazy<PackageInstallerService>,
	private val ackpineServiceProviders: Lazy<Set<AckpineServiceProvider>>,
	private val lastUpdateTimestampDao: LastUpdateTimestampDao,
	private val installSessionDao: InstallSessionDao,
	private val sessionDao: SessionDao,
	private val sessionProgressDao: SessionProgressDao,
	private val nativeSessionIdDao: NativeSessionIdDao,
	private val installPreapprovalDao: InstallPreapprovalDao,
	private val installConstraintsDao: InstallConstraintsDao,
	private val executor: Executor,
	private val handler: Handler,
	private val sessionCallbackHandler: Lazy<Handler>
) : InstallSessionFactory {

	override fun create(
		parameters: InstallParameters,
		id: UUID,
		notificationId: Int,
		dbWriteSemaphore: BinarySemaphore
	): CompletableProgressSession<InstallFailure> = when (parameters.installerType) {
		InstallerType.INTENT_BASED -> IntentBasedInstallSession(
			applicationContext,
			apk = parameters.apks.toList().singleOrNull() ?: throw SplitPackagesNotSupportedException(),
			id,
			initialState = Session.State.Pending,
			initialProgress = Progress(),
			parameters.confirmation,
			resolveNotificationData(parameters.notificationData, parameters.name),
			lastUpdateTimestampDao, sessionDao,
			sessionFailureDao = installSessionDao,
			sessionProgressDao, executor, handler, notificationId, dbWriteSemaphore
		)

		InstallerType.SESSION_BASED -> withPackageInstallerService(
			id,
			runCatching {
				parameters
					.pluginContainer
					.getPlugins()
					.mapKeys { (pluginClass, _) -> AckpinePluginCache.get(pluginClass) }
			}
		) { packageInstallerService ->
			SessionBasedInstallSession(
				applicationContext,
				packageInstallerService,
				apks = parameters.apks.toList(),
				id,
				initialState = Session.State.Pending,
				initialProgress = Progress(),
				parameters.confirmation,
				resolveNotificationData(parameters.notificationData, parameters.name),
				parameters.requireUserAction, parameters.installMode, parameters.preapproval, parameters.constraints,
				parameters.requestUpdateOwnership, parameters.packageSource,
				sessionDao,
				sessionFailureDao = installSessionDao,
				sessionProgressDao, nativeSessionIdDao, installPreapprovalDao, installConstraintsDao,
				executor, handler, sessionCallbackHandler.value,
				nativeSessionId = -1,
				notificationId,
				commitAttemptsCount = 0,
				isPreapproved = false,
				dbWriteSemaphore
			)
		}
	}

	override fun create(
		session: SessionEntity.InstallSession,
		completeIfSucceeded: Boolean
	): CompletableProgressSession<InstallFailure> = when (session.installerType) {
		InstallerType.INTENT_BASED -> createIntentBasedInstallSession(session, completeIfSucceeded)
		InstallerType.SESSION_BASED -> createSessionBasedInstallSession(session, completeIfSucceeded)
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

	private fun <R : CompletableProgressSession<InstallFailure>> withPackageInstallerService(
		sessionId: UUID,
		pluginsSet: Result<Map<AckpinePlugin<*>, AckpinePlugin.Parameters>>,
		sessionFactory: (PackageInstallerService) -> R
	): R {
		val packageInstallerService = pluginsSet.mapCatching { plugins ->
			if (plugins.isEmpty()) {
				return sessionFactory(defaultPackageInstallerService.value)
			}
			val pluginIds = plugins.keys.map { plugin -> plugin.id }
			ackpineServiceProviders
				.value
				.filter { provider -> provider.pluginId in pluginIds }
				.firstNotNullOfOrNull { provider -> provider.get<PackageInstallerService>(applicationContext) }
				?.also { service ->
					plugins
						.values
						.filterNot { params -> params is AckpinePlugin.Parameters.None }
						.forEach { params -> service.applyParameters(sessionId, params) }
				}
		}
		val session = sessionFactory(
			packageInstallerService.getOrNull() ?: defaultPackageInstallerService.value
		)
		packageInstallerService.onFailure { throwable ->
			when (throwable) {
				is Error -> session.completeExceptionally(RuntimeException(throwable))
				is Exception -> session.completeExceptionally(throwable)
			}
		}
		return session
	}

	private fun createIntentBasedInstallSession(
		installSession: SessionEntity.InstallSession,
		completeIfSucceeded: Boolean
	): IntentBasedInstallSession {
		val id = UUID.fromString(installSession.session.id)
		val initialState = installSession.getState(installSessionDao)
		val initialProgress = installSession.getProgress(sessionProgressDao)
		val session = IntentBasedInstallSession(
			applicationContext,
			apk = installSession.uris.singleOrNull()?.toUri() ?: throw SplitPackagesNotSupportedException(),
			id, initialState, initialProgress,
			installSession.session.confirmation, installSession.getNotificationData(),
			lastUpdateTimestampDao, sessionDao,
			sessionFailureDao = installSessionDao,
			sessionProgressDao, executor, handler, installSession.notificationId!!,
			BinarySemaphore()
		)
		if (!completeIfSucceeded || initialState.isTerminal) {
			return session
		}
		// Though it somewhat helps with self-update sessions, it's still faulty:
		// if app is force-stopped while the session is committed (not confirmed) and in the meantime
		// another installer updates the app, this session will be viewed as completed successfully.
		// We can check that initiating installer package is the same as ours, but then if this session
		// was successful, and before launching the app again it was updated by another installer,
		// the session will be stuck as committed. Sadly, without centralized system
		// sessions repository, such as android.content.pm.PackageInstaller, we can't reliably determine
		// whether the intent-based Ackpine session was really successful.
		val packageName = installSession.packageName.orEmpty()
		val lastUpdateTimestamp = installSession.lastUpdateTimestamp ?: Long.MAX_VALUE
		val isSelfUpdate = initialState is Committed && applicationContext.packageName == packageName
		val isLastUpdateTimestampUpdated = getLastSelfUpdateTimestamp() > lastUpdateTimestamp
		if (isSelfUpdate && isLastUpdateTimestampUpdated) {
			session.complete(Succeeded)
			lastUpdateTimestampDao.setLastUpdateTimestamp(id.toString(), getLastSelfUpdateTimestamp())
		}
		return session
	}

	private fun createSessionBasedInstallSession(
		installSession: SessionEntity.InstallSession,
		completeIfSucceeded: Boolean
	): SessionBasedInstallSession {
		val sessionId = UUID.fromString(installSession.session.id)
		val initialState = installSession.getState(installSessionDao)
		val initialProgress = installSession.getProgress(sessionProgressDao)
		val nativeSessionId = installSession.nativeSessionId ?: -1
		val session = withPackageInstallerService(sessionId, installSession.getPlugins()) { packageInstallerService ->
			SessionBasedInstallSession(
				applicationContext,
				packageInstallerService,
				apks = installSession.uris.map(String::toUri),
				sessionId,
				initialState, initialProgress,
				installSession.session.confirmation, installSession.getNotificationData(),
				installSession.session.requireUserAction, installSession.getInstallMode(),
				installSession.getPreapproval(), installSession.getConstraints(),
				requestUpdateOwnership = installSession.requestUpdateOwnership == true,
				packageSource = installSession.packageSource ?: PackageSource.Unspecified,
				sessionDao,
				sessionFailureDao = installSessionDao,
				sessionProgressDao, nativeSessionIdDao, installPreapprovalDao, installConstraintsDao,
				executor, handler, sessionCallbackHandler.value, nativeSessionId, installSession.notificationId!!,
				commitAttemptsCount = installSession.constraints?.commitAttemptsCount ?: 0,
				isPreapproved = installSession.preapproval?.isPreapproved == true,
				dbWriteSemaphore = BinarySemaphore()
			)
		}
		if (session.isCompleted) {
			return session
		}
		if (!completeIfSucceeded || initialState.isTerminal) {
			return session
		}
		val commitProgressValue = CommitProgressValueHolder.get(applicationContext)
		val progressThreshold = (commitProgressValue * PROGRESS_MAX).toInt()
		val isInstallationOngoingOrCompleted = initialProgress.progress >= progressThreshold
		// We can't rely on session's progress on API 31-32 with requireUserAction == false, so we don't block clients
		// from committing on these versions if user's confirmation was already launched previously.
		val shouldBlockCommitIfIsOngoing = installSession.session.requireUserAction
				|| Build.VERSION.SDK_INT !in 31..32
				|| installSession.wasConfirmationLaunched != true
		if (shouldBlockCommitIfIsOngoing && isInstallationOngoingOrCompleted) {
			session.notifyCommitted() // block clients from committing
		}
		// If app is killed while installing but system installer activity remains visible,
		// session is stuck in Committed state after new process start.
		// Fails are guaranteed to be handled by PackageInstallerStatusReceiver (in case of self-update
		// success is not handled), so if native session doesn't exist, it can only mean that it succeeded.
		// There may be latency from the receiver, so we delay this to allow the receiver to kick in.
		val packageInstaller = applicationContext.packageManager.packageInstaller
		if (initialState is Committed && packageInstaller.getSessionInfo(nativeSessionId) == null) {
			handler.postDelayed({ session.complete(Succeeded) }, 2000)
		}
		return session
	}

	private fun getLastSelfUpdateTimestamp(): Long {
		return applicationContext.packageManager.getPackageInfo(applicationContext.packageName, 0).lastUpdateTime
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