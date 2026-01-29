/*
 * Copyright (C) 2026 Ilya Fomichev
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

package ru.solrudev.ackpine.remote

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Context.BIND_AUTO_CREATE
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import ru.solrudev.ackpine.installer.PackageInstaller
import ru.solrudev.ackpine.uninstaller.PackageUninstaller
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * A service allowing to use Ackpine APIs via IPC.
 */
public class AckpineRemoteService internal constructor(private val service: IAckpineRemoteService) {

	/**
	 * Returns a singleton instance of [RemotePackageInstaller].
	 */
	public val packageInstaller: RemotePackageInstaller by lazy {
		RemotePackageInstaller(service.packageInstaller)
	}

	/**
	 * Returns a singleton instance of [RemotePackageUninstaller].
	 */
	public val packageUninstaller: RemotePackageUninstaller by lazy {
		RemotePackageUninstaller(service.packageUninstaller)
	}

	public companion object {

		/**
		 * Binds to [AckpineRemoteService] and returns an [AckpineRemoteServiceConnection].
		 * @param packageName the name of the package that the [AckpineRemoteService] exists in.
		 * @param onDisconnected an action which will be invoked when the service is disconnected.
		 */
		public fun bind(
			context: Context,
			packageName: String,
			onDisconnected: () -> Unit = {}
		): AckpineRemoteServiceConnection {
			val connection = AckpineRemoteServiceConnection(context, onDisconnected)
			context.bindService(
				Intent().setComponent(ComponentName(packageName, AckpineService::class.java.name)),
				connection,
				BIND_AUTO_CREATE
			)
			return connection
		}

		/**
		 * Binds to [AckpineRemoteService] and unbinds after exiting the [block].
		 * @param packageName the name of the package that the [AckpineRemoteService] exists in.
		 * @param onDisconnected an action which will be invoked when the service is disconnected.
		 */
		@OptIn(ExperimentalContracts::class)
		public inline fun <R> use(
			context: Context,
			packageName: String,
			noinline onDisconnected: () -> Unit = {},
			block: (AckpineRemoteServiceConnection) -> R
		): R {
			contract {
				callsInPlace(block, InvocationKind.EXACTLY_ONCE)
			}
			val connection = bind(context, packageName, onDisconnected)
			try {
				return block(connection)
			} finally {
				context.unbindService(connection)
			}
		}
	}
}

/**
 * A [ServiceConnection] for [AckpineRemoteService].
 */
public class AckpineRemoteServiceConnection internal constructor(
	private val context: Context,
	private val onDisconnected: () -> Unit
) : ServiceConnection {

	private val serviceFlow = MutableStateFlow<Any?>(INITIAL_VALUE)

	override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
		if (service == null || !service.pingBinder()) {
			serviceFlow.value = null
			return
		}
		val serviceFromBinder = IAckpineRemoteService.Stub.asInterface(service)
		serviceFlow.value = serviceFromBinder?.let(::AckpineRemoteService)
	}

	override fun onServiceDisconnected(name: ComponentName?) {
		serviceFlow.value = null
		context.unbindService(this)
		onDisconnected()
	}

	/**
	 * Suspends with specified [timeout] (15 seconds by default) until [AckpineRemoteService] is connected,
	 * and then returns it.
	 */
	public suspend fun awaitService(timeout: Duration = 15.seconds): AckpineRemoteService {
		return withContext(Dispatchers.Default.limitedParallelism(1)) {
			withTimeoutOrNull(timeout) {
				@Suppress("UNCHECKED_CAST")
				serviceFlow
					.filter { it !== INITIAL_VALUE }
					.first() as AckpineRemoteService?
			} ?: error("AckpineRemoteService failed to bind")
		}
	}
}

private val INITIAL_VALUE = Any()

internal class AckpineService : Service() {
	private val binder by lazy { AckpineRemoteServiceBinder(applicationContext) }
	override fun onBind(intent: Intent?): IBinder = binder
}

private class AckpineRemoteServiceBinder(private val context: Context) : IAckpineRemoteService.Stub() {
	private val installer by lazy { RemotePackageInstallerImpl(PackageInstaller.getInstance(context)) }
	private val uninstaller by lazy { RemotePackageUninstallerImpl(PackageUninstaller.getInstance(context)) }
	override fun getPackageInstaller() = installer
	override fun getPackageUninstaller() = uninstaller
}