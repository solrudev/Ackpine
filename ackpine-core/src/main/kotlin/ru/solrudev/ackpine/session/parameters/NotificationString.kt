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

package ru.solrudev.ackpine.session.parameters

import android.content.Context
import androidx.annotation.StringRes
import java.io.Serializable

/**
 * String for a session's [confirmation notification][NotificationData].
 */
public sealed interface NotificationString : Serializable {

	/**
	 * Returns whether this string represents a default string.
	 */
	public val isDefault: Boolean
		get() = this is Default

	/**
	 * Returns whether this string is empty.
	 */
	public val isEmpty: Boolean
		get() = this is Empty

	/**
	 * Returns whether this string represents a hardcoded string.
	 */
	public val isRaw: Boolean
		get() = this is Raw

	/**
	 * Returns whether this string represents a resource string.
	 */
	public val isResource: Boolean
		get() = this is Resource

	/**
	 * Resolves string value for a given [context].
	 */
	public fun resolve(context: Context): String

	public companion object {

		/**
		 * Creates a default [NotificationString].
		 */
		@JvmStatic
		public fun default(): NotificationString = Default

		/**
		 * Creates an empty [NotificationString].
		 */
		@JvmStatic
		public fun empty(): NotificationString = Empty

		/**
		 * Creates [NotificationString] with a hardcoded value.
		 */
		@JvmStatic
		public fun raw(value: String): NotificationString {
			if (value.isEmpty()) {
				return Empty
			}
			return Raw(value)
		}

		/**
		 * Creates [NotificationString] represented by Android resource string with optional arguments. Arguments can be
		 * [NotificationStrings][NotificationString] as well.
		 */
		@JvmStatic
		public fun resource(@StringRes stringId: Int, vararg args: Serializable): NotificationString {
			return Resource(stringId, args)
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