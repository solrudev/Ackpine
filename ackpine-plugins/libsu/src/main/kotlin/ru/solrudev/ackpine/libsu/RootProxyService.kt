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

package ru.solrudev.ackpine.libsu

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Parcel
import androidx.annotation.RestrictTo
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ipc.RootService
import ru.solrudev.ackpine.libsu.RootProxyService.Companion.BINDER_DESCRIPTOR
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class RootProxyService : RootService() {

	override fun onBind(intent: Intent): IBinder = RootProxyBinder

	internal companion object {

		@JvmSynthetic
		internal const val BINDER_DESCRIPTOR = "ru.solrudev.ackpine.libsu.RootProxyService"

		private val handler = Handler(Looper.getMainLooper())

		@JvmSynthetic
		internal fun bind(context: Context): IBinder {
			Shell.getCachedShell()?.close()
			if (!Shell.getShell().isRoot) {
				error("Can't open root shell")
			}
			val intent = Intent(context, RootProxyService::class.java)
			val connection = RootServiceConnection()
			handler.post {
				bind(intent, connection)
			}
			return connection.getService()
		}
	}
}

private object RootProxyBinder : Binder() {
	override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
		if (code == INTERFACE_TRANSACTION) {
			reply?.writeString(BINDER_DESCRIPTOR)
			return true
		}
		if (code != FIRST_CALL_TRANSACTION) {
			return super.onTransact(code, data, reply, flags)
		}
		data.enforceInterface(BINDER_DESCRIPTOR)
		val target = data.readStrongBinder() ?: return false
		val transactionCode = data.readInt()
		val transactionFlags = data.readInt()
		val forwardedData = Parcel.obtain()
		return try {
			forwardedData.appendFrom(data, data.dataPosition(), data.dataAvail())
			target.transact(transactionCode, forwardedData, reply, transactionFlags)
		} finally {
			forwardedData.recycle()
		}
	}
}

private class RootServiceConnection : ServiceConnection {

	private val latch = CountDownLatch(1)

	@Volatile
	private var rootService: IBinder? = null

	@Volatile
	private var failure: Throwable? = null

	fun getService(): IBinder {
		try {
			if (!latch.await(20, TimeUnit.SECONDS)) {
				error("Timed out waiting 20s for root service binding")
			}
		} catch (exception: InterruptedException) {
			Thread.currentThread().interrupt()
			throw IllegalStateException("Interrupted while waiting for root service binding", exception)
		}
		failure?.let { throwable ->
			throw throwable
		}
		return checkNotNull(rootService) {
			"Root service binding completed without a binder"
		}
	}

	override fun onServiceConnected(name: ComponentName, service: IBinder) {
		rootService = service
		latch.countDown()
	}

	override fun onServiceDisconnected(name: ComponentName) {
		rootService = null
	}

	override fun onBindingDied(name: ComponentName) {
		failure = IllegalStateException("Root service binding died before connection was established")
		latch.countDown()
	}
}