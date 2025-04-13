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

package ru.solrudev.ackpine.uninstaller

import android.content.Context
import android.os.Handler
import com.google.common.util.concurrent.ListenableFuture
import ru.solrudev.ackpine.Ackpine
import ru.solrudev.ackpine.impl.database.AckpineDatabase
import ru.solrudev.ackpine.impl.uninstaller.PackageUninstallerImpl
import ru.solrudev.ackpine.impl.uninstaller.UninstallSessionFactoryImpl
import ru.solrudev.ackpine.plugin.AckpinePlugin
import ru.solrudev.ackpine.plugin.AckpinePluginRegistry
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.uninstaller.parameters.UninstallParameters
import java.util.UUID
import java.util.concurrent.Executor

/**
 * Provides the ability to uninstall applications from the device.
 *
 * In essence, it's a repository of [Sessions][Session].
 */
public interface PackageUninstaller {

	/**
	 * Creates a uninstall session with provided [parameters].The returned session is in
	 * [pending][Session.State.Pending] state.
	 *
	 * @param parameters an instance of [UninstallParameters] which configures the uninstall session.
	 * @return [Session]
	 */
	public fun createSession(parameters: UninstallParameters): Session<UninstallFailure>

	/**
	 * Returns a [uninstall session][Session] which matches the provided [sessionId], or `null` if not found.
	 *
	 * Cancelling this future is a no-op.
	 *
	 * @return [ListenableFuture] of [Session].
	 */
	public fun getSessionAsync(sessionId: UUID): ListenableFuture<out Session<UninstallFailure>?>

	/**
	 * Returns all [uninstall sessions][Session] tracked by this [PackageUninstaller], [active][Session.isActive] or
	 * not.
	 *
	 * Cancelling this future is a no-op.
	 *
	 * @return [ListenableFuture] of [Sessions][Session] list.
	 */
	public fun getSessionsAsync(): ListenableFuture<out List<Session<UninstallFailure>>>

	/**
	 * Returns all [active][Session.isActive] [uninstall sessions][Session] tracked by this [PackageUninstaller].
	 *
	 * Cancelling this future is a no-op.
	 *
	 * @return [ListenableFuture] of [Sessions][Session] list.
	 */
	public fun getActiveSessionsAsync(): ListenableFuture<out List<Session<UninstallFailure>>>

	public companion object {

		private val lock = Any()

		@Volatile
		private var packageUninstaller: PackageUninstallerImpl? = null

		/**
		 * Retrieves the default singleton instance of [PackageUninstaller].
		 *
		 * @param context a [Context] for on-demand initialization.
		 * @return The singleton instance of [PackageUninstaller].
		 */
		@JvmStatic
		public fun getInstance(context: Context): PackageUninstaller {
			return getImpl(context)
		}

		@JvmSynthetic
		internal fun getImpl(context: Context): PackageUninstallerImpl {
			var instance = packageUninstaller
			if (instance != null) {
				return instance
			}
			synchronized(lock) {
				instance = packageUninstaller
				if (instance == null) {
					instance = create(context)
					packageUninstaller = instance
				}
			}
			return instance!!
		}

		private fun create(context: Context): PackageUninstallerImpl {
			AckpinePluginRegistry.register(PackageUninstallerPlugin)
			val database = AckpineDatabase.getInstance(context.applicationContext, PackageUninstallerPlugin.executor)
			return PackageUninstallerImpl(
				database.uninstallSessionDao(),
				PackageUninstallerPlugin.executor,
				UninstallSessionFactoryImpl(
					context.applicationContext,
					database.sessionDao(),
					database.uninstallSessionDao(),
					PackageUninstallerPlugin.executor,
					Handler(context.mainLooper)
				),
				uuidFactory = UUID::randomUUID,
				notificationIdFactory = Ackpine.globalNotificationId::incrementAndGet
			)
		}
	}
}

private object PackageUninstallerPlugin : AckpinePlugin {

	lateinit var executor: Executor
		private set

	override fun setExecutor(executor: Executor) {
		this.executor = executor
	}
}