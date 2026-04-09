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

@file:Suppress("Unused")

package ru.solrudev.ackpine.splits.testutil

import android.os.ParcelFileDescriptor
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.RealObject
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowParcelFileDescriptor
import java.io.File
import java.io.FileDescriptor
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@Implements(ParcelFileDescriptor::class)
class ShadowBlockingParcelFileDescriptor : ShadowParcelFileDescriptor() {

	@RealObject
	private lateinit var realPfd: ParcelFileDescriptor

	var isInvalid = false
	private var pipe: Pipe = Pipe.None

	companion object {

		@Implementation(methodName = "createPipe")
		@JvmStatic
		fun blockingCreatePipe(): Array<ParcelFileDescriptor> {
			val file = File.createTempFile("pipe-", null)
			file.deleteOnExit()
			val readSide = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
			val writeSide = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_WRITE)
			val latch = CountDownLatch(1)
			Shadow.extract<ShadowBlockingParcelFileDescriptor>(readSide).pipe = Pipe.Reader(latch)
			Shadow.extract<ShadowBlockingParcelFileDescriptor>(writeSide).pipe = Pipe.Writer(latch)
			return arrayOf(readSide, writeSide)
		}

		@Implementation(methodName = "createReliablePipe")
		@JvmStatic
		fun blockingCreateReliablePipe() = blockingCreatePipe()

	}

	@Implementation(methodName = "getFd")
	override fun getFd(): Int {
		if (isInvalid) {
			return -1
		}
		return super.getFd()
	}

	@Implementation(methodName = "getFileDescriptor")
	@Throws(IOException::class)
	fun blockingGetFileDescriptor(): FileDescriptor = when (val pipe = pipe) {
		is Pipe.Reader -> {
			pipe.latch.await(5, TimeUnit.SECONDS)
			val pfd = realPfd.dup()
			this.pipe = Pipe.Resolved(pfd)
			pfd.fileDescriptor
		}

		is Pipe.Resolved -> pipe.pfd.fileDescriptor
		else -> super.getFileDescriptor()
	}

	@Implementation(methodName = "close")
	@Throws(IOException::class)
	fun blockingClose() {
		when (val pipe = pipe) {
			is Pipe.Writer -> pipe.latch.countDown()
			is Pipe.Resolved -> pipe.pfd.close()
			else -> {}
		}
		pipe = Pipe.None
		super.close()
	}

	private sealed interface Pipe {
		object None : Pipe
		class Reader(val latch: CountDownLatch) : Pipe
		class Writer(val latch: CountDownLatch) : Pipe
		class Resolved(val pfd: ParcelFileDescriptor) : Pipe
	}
}