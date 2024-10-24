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

@file:Suppress("Unused")

package ru.solrudev.ackpine.session.parameters

import android.content.Context
import androidx.annotation.StringRes
import java.io.Serializable

/**
 * String which can be resolved at use site.
 */
public interface NotificationString : Serializable {

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
		 * Creates an anonymous instance of [NotificationString.Resource], which is a [NotificationString] represented by
		 * Android resource string with optional arguments. Arguments can be [NotificationStrings][NotificationString]
		 * as well.
		 *
		 * This factory is meant to create **only** transient strings, i.e. not persisted in storage. For persisted
		 * strings [NotificationString.Resource] should be explicitly subclassed. Example:
		 * ```
		 * object MessageString : NotificationString.Resource(R.string.message)
		 * class ErrorString(error: String) : NotificationString.Resource(R.string.error, error)
		 * ```
		 */
		@JvmStatic
		public fun resource(@StringRes stringId: Int, vararg args: Serializable): NotificationString {
			return object : Resource(stringId, args) {}
		}
	}

	/**
	 * [NotificationString] represented by Android resource string with optional arguments. Arguments can be
	 * [NotificationStrings][NotificationString] as well.
	 *
	 * Should be explicitly subclassed to ensure stable persistence. Example:
	 * ```
	 * object MessageString : NotificationString.Resource(R.string.message)
	 * class ErrorString(error: String) : NotificationString.Resource(R.string.error, error)
	 * ```
	 * For transient strings, i.e. not persisted in storage, you can use [NotificationString.resource] factory.
	 */
	public abstract class Resource(
		@[StringRes Transient] private val stringId: Int,
		private vararg val args: Serializable
	) : NotificationString {

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
			return args.contentEquals(other.args)
		}

		override fun hashCode(): Int {
			var result = stringId
			result = 31 * result + args.contentHashCode()
			return result
		}

		private companion object {
			private const val serialVersionUID = -7766769726170724379L
		}
	}
}

private data object Empty : NotificationString {
	private const val serialVersionUID = 5194188194930148316L
	override fun resolve(context: Context): String = ""
}

private data class Raw(val value: String) : NotificationString {
	override fun resolve(context: Context): String = value
	private companion object {
		private const val serialVersionUID = -6824736411987160679L
	}
}