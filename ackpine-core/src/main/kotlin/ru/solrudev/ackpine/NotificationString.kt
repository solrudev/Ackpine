package ru.solrudev.ackpine

import android.content.Context
import androidx.annotation.StringRes
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable

public sealed interface NotificationString : Serializable {

	public val isDefault: Boolean
		get() = this is Default

	public val isEmpty: Boolean
		get() = this is Empty

	public val isRaw: Boolean
		get() = this is Raw

	public val isResource: Boolean
		get() = this is Resource

	public fun resolve(context: Context): String

	public companion object {

		@JvmStatic
		public fun default(): NotificationString = Default

		@JvmStatic
		public fun empty(): NotificationString = Empty

		@JvmStatic
		public fun raw(value: String): NotificationString {
			if (value.isEmpty()) {
				return Empty
			}
			return Raw(value)
		}

		@JvmStatic
		public fun resource(@StringRes stringId: Int, vararg args: Serializable): NotificationString {
			return Resource(stringId, args)
		}

		@JvmSynthetic
		internal fun fromByteArray(byteArray: ByteArray): NotificationString {
			ByteArrayInputStream(byteArray).use { byteArrayInputStream ->
				ObjectInputStream(byteArrayInputStream).use { objectInputStream ->
					return objectInputStream.readObject() as NotificationString
				}
			}
		}

		@JvmSynthetic
		internal fun toByteArray(notificationString: NotificationString): ByteArray =
			ByteArrayOutputStream().use { byteArrayOutputStream ->
				ObjectOutputStream(byteArrayOutputStream).use { objectOutputStream ->
					objectOutputStream.writeObject(notificationString)
					objectOutputStream.flush()
				}
				return byteArrayOutputStream.toByteArray()
			}
	}
}

private data object Default : NotificationString {
	override fun resolve(context: Context): String = ""
}

private data object Empty : NotificationString {
	override fun resolve(context: Context): String = ""
}

private data class Raw(val value: String) : NotificationString {
	override fun resolve(context: Context): String = value
}

private data class Resource(@StringRes val stringId: Int, val args: Array<out Serializable>) : NotificationString {

	override fun resolve(context: Context): String = context.getString(stringId, *resolveArgs(context))

	private fun resolveArgs(context: Context): Array<Serializable> = args.map { argument ->
		if (argument is NotificationString) {
			argument.resolve(context)
		} else {
			argument
		}
	}.toTypedArray()

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false
		other as Resource
		if (stringId != other.stringId) return false
		if (!args.contentEquals(other.args)) return false
		return true
	}

	override fun hashCode(): Int {
		var result = stringId
		result = 31 * result + args.contentHashCode()
		return result
	}
}