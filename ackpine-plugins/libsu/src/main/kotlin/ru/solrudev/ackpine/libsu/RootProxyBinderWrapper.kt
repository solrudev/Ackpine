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

import android.os.IBinder
import android.os.Parcel
import androidx.annotation.RestrictTo
import java.io.FileDescriptor

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class RootProxyBinderWrapper(
	private val rootService: IBinder,
	private val original: IBinder
) : IBinder {

	override fun transact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
		val wrappedData = Parcel.obtain()
		try {
			wrappedData.writeInterfaceToken(RootProxyService.BINDER_DESCRIPTOR)
			wrappedData.writeStrongBinder(original)
			wrappedData.writeInt(code)
			wrappedData.writeInt(flags)
			wrappedData.appendFrom(data, 0, data.dataSize())
			return rootService.transact(IBinder.FIRST_CALL_TRANSACTION, wrappedData, reply, 0)
		} finally {
			wrappedData.recycle()
		}
	}

	override fun getInterfaceDescriptor() = original.interfaceDescriptor
	override fun pingBinder() = original.pingBinder()
	override fun isBinderAlive() = original.isBinderAlive
	override fun queryLocalInterface(descriptor: String) = null
	override fun dump(fd: FileDescriptor, args: Array<out String>?) = original.dump(fd, args)
	override fun dumpAsync(fd: FileDescriptor, args: Array<out String>?) = original.dumpAsync(fd, args)
	override fun linkToDeath(recipient: IBinder.DeathRecipient, flags: Int) = original.linkToDeath(recipient, flags)
	override fun unlinkToDeath(recipient: IBinder.DeathRecipient, flags: Int) = original.unlinkToDeath(recipient, flags)
}