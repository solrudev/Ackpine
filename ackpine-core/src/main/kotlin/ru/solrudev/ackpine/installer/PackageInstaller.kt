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

package ru.solrudev.ackpine.installer

import android.content.Context
import android.os.Handler
import com.google.common.util.concurrent.ListenableFuture
import ru.solrudev.ackpine.helpers.globalNotificationId
import ru.solrudev.ackpine.impl.database.AckpineDatabase
import ru.solrudev.ackpine.impl.installer.InstallSessionFactoryImpl
import ru.solrudev.ackpine.impl.installer.PackageInstallerImpl
import ru.solrudev.ackpine.installer.parameters.InstallParameters
import ru.solrudev.ackpine.plugin.AckpinePlugin
import ru.solrudev.ackpine.plugin.AckpinePluginRegistry
import ru.solrudev.ackpine.session.ProgressSession
import ru.solrudev.ackpine.session.Session
import java.util.UUID
import java.util.concurrent.Executor

/**
 * Provides the ability to install applications on the device.
 * This includes support for monolithic APKs and split APKs.
 *
 * In essence, it's a repository of [ProgressSessions][ProgressSession].
 */
public interface PackageInstaller {

	/**
	 * Creates an install session with provided [parameters]. The returned session is in
	 * [pending][Session.State.Pending] state.
	 *
	 * @param parameters an instance of [InstallParameters] which configures the install session.
	 * @return [ProgressSession]
	 */
	public fun createSession(parameters: InstallParameters): ProgressSession<InstallFailure>

	/**
	 * Returns an [install session][ProgressSession] which matches the provided [sessionId], or `null` if not found.
	 *
	 * Cancelling this future is a no-op.
	 *
	 * @return [ListenableFuture] of [ProgressSession].
	 */
	public fun getSessionAsync(sessionId: UUID): ListenableFuture<ProgressSession<InstallFailure>?>

	/**
	 * Returns all [install sessions][ProgressSession] tracked by this [PackageInstaller],
	 * [active][ProgressSession.isActive] or not.
	 *
	 * Cancelling this future is a no-op.
	 *
	 * @return [ListenableFuture] of [ProgressSessions][ProgressSession] list.
	 */
	public fun getSessionsAsync(): ListenableFuture<List<ProgressSession<InstallFailure>>>

	/**
	 * Returns all [active][ProgressSession.isActive] [install sessions][ProgressSession] tracked by this
	 * [PackageInstaller].
	 *
	 * Cancelling this future is a no-op.
	 *
	 * @return [ListenableFuture] of [ProgressSessions][ProgressSession] list.
	 */
	public fun getActiveSessionsAsync(): ListenableFuture<List<ProgressSession<InstallFailure>>>

	public companion object {

		private val lock = Any()

		@Volatile
		private var packageInstaller: PackageInstaller? = null

		/**
		 * Retrieves the default singleton instance of [PackageInstaller].
		 *
		 * @param context a [Context] for on-demand initialization.
		 * @return The singleton instance of [PackageInstaller].
		 */
		@JvmStatic
		public fun getInstance(context: Context): PackageInstaller {
			var instance = packageInstaller
			if (instance != null) {
				return instance
			}
			synchronized(lock) {
				instance = packageInstaller
				if (instance == null) {
					instance = create(context)
					packageInstaller = instance
				}
			}
			return instance!!
		}

		private fun create(context: Context): PackageInstaller {
			AckpinePluginRegistry.register(PackageInstallerPlugin)
			val database = AckpineDatabase.getInstance(context.applicationContext, PackageInstallerPlugin.executor)
			return PackageInstallerImpl(
				database.installSessionDao(),
				database.sessionProgressDao(),
				PackageInstallerPlugin.executor,
				InstallSessionFactoryImpl(
					context.applicationContext,
					database.sessionDao(),
					database.installSessionDao(),
					database.sessionProgressDao(),
					database.nativeSessionIdDao(),
					PackageInstallerPlugin.executor,
					Handler(context.mainLooper)
				),
				uuidFactory = UUID::randomUUID,
				notificationIdFactory = globalNotificationId::incrementAndGet
			)
		}
	}
}

private object PackageInstallerPlugin : AckpinePlugin {

	lateinit var executor: Executor
		private set

	override fun setExecutor(executor: Executor) {
		this.executor = executor
	}
}