package ru.solrudev.ackpine.impl.database.converters

import java.io.*

@JvmSynthetic
internal inline fun <reified T : Serializable> ByteArray.deserialize(): T {
	ByteArrayInputStream(this).use { byteArrayInputStream ->
		ObjectInputStream(byteArrayInputStream).use { objectInputStream ->
			return objectInputStream.readObject() as T
		}
	}
}

@JvmSynthetic
internal inline fun <reified T : Serializable> T.serialize(): ByteArray =
	ByteArrayOutputStream().use { byteArrayOutputStream ->
		ObjectOutputStream(byteArrayOutputStream).use { objectOutputStream ->
			objectOutputStream.writeObject(this)
			objectOutputStream.flush()
		}
		return byteArrayOutputStream.toByteArray()
	}