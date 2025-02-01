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

package ru.solrudev.ackpine.impl.database.converters

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable

@Suppress("UNCHECKED_CAST")
@JvmSynthetic
internal fun <T : Serializable> ByteArray.deserialize(): T {
	ByteArrayInputStream(this).use { byteArrayInputStream ->
		ObjectInputStream(byteArrayInputStream).use { objectInputStream ->
			return objectInputStream.readObject() as T
		}
	}
}

@JvmSynthetic
internal fun <T : Serializable> T.serialize(): ByteArray =
	ByteArrayOutputStream().use { byteArrayOutputStream ->
		ObjectOutputStream(byteArrayOutputStream).use { objectOutputStream ->
			objectOutputStream.writeObject(this)
			objectOutputStream.flush()
		}
		return byteArrayOutputStream.toByteArray()
	}