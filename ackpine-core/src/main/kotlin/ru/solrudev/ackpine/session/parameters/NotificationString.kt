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
 * String for a session's [confirmation notification][NotificationData].
 */
@Deprecated(
	message = "This class cannot provide persistence stability of string resources in case of string " +
			"resources updates. Use ResolvableString to provide strings to notifications. " +
			"NotificationString will be removed in next minor release.",
	level = DeprecationLevel.ERROR
)
public sealed interface NotificationString : Serializable {

	/**
	 * Returns whether this string represents a default string.
	 */
	@Deprecated(message = "This property is removed from ResolvableString.", level = DeprecationLevel.ERROR)
	public val isDefault: Boolean
		get() = this is Default

	/**
	 * Returns whether this string is empty.
	 */
	@Deprecated(
		message = "This class cannot provide persistence stability of string resources in case of string " +
				"resources updates. Use ResolvableString to provide strings to notifications. " +
				"NotificationString will be removed in next minor release.",
		level = DeprecationLevel.ERROR,
		replaceWith = ReplaceWith(expression = "ResolvableString.isEmpty")
	)
	public val isEmpty: Boolean
		get() = this is Empty

	/**
	 * Returns whether this string represents a hardcoded string.
	 */
	@Deprecated(
		message = "This class cannot provide persistence stability of string resources in case of string " +
				"resources updates. Use ResolvableString to provide strings to notifications. " +
				"NotificationString will be removed in next minor release.",
		level = DeprecationLevel.ERROR,
		replaceWith = ReplaceWith(expression = "ResolvableString.isRaw")
	)
	public val isRaw: Boolean
		get() = this is Raw

	/**
	 * Returns whether this string represents a resource string.
	 */
	@Deprecated(
		message = "This class cannot provide persistence stability of string resources in case of string " +
				"resources updates. Use ResolvableString to provide strings to notifications. " +
				"NotificationString will be removed in next minor release.",
		level = DeprecationLevel.ERROR,
		replaceWith = ReplaceWith(expression = "ResolvableString.isResource")
	)
	public val isResource: Boolean
		get() = this is Resource

	/**
	 * Resolves string value for a given [context].
	 */
	public fun resolve(context: Context): String

	@Deprecated(
		message = "This class cannot provide persistence stability of string resources in case of string " +
				"resources updates. Use ResolvableString to provide strings to notifications. " +
				"NotificationString will be removed in next minor release.",
		level = DeprecationLevel.ERROR
	)
	public companion object {

		/**
		 * Creates a default [NotificationString].
		 */
		@JvmStatic
		@Suppress("DEPRECATION_ERROR")
		@Deprecated(
			message = "This class cannot provide persistence stability of string resources in case of string " +
					"resources updates. Use ResolvableString to provide strings to notifications. " +
					"NotificationString will be removed in next minor release.",
			level = DeprecationLevel.ERROR
		)
		public fun default(): NotificationString = Default

		/**
		 * Creates an empty [NotificationString].
		 */
		@JvmStatic
		@Suppress("DEPRECATION_ERROR")
		@Deprecated(
			message = "This class cannot provide persistence stability of string resources in case of string " +
					"resources updates. Use ResolvableString to provide strings to notifications. " +
					"NotificationString will be removed in next minor release.",
			level = DeprecationLevel.ERROR,
			replaceWith = ReplaceWith(
				expression = "ResolvableString.empty()",
				imports = ["ru.solrudev.ackpine.resources.ResolvableString"]
			)
		)
		public fun empty(): NotificationString = Empty

		/**
		 * Creates [NotificationString] with a hardcoded value.
		 */
		@JvmStatic
		@Suppress("DEPRECATION_ERROR")
		@Deprecated(
			message = "This class cannot provide persistence stability of string resources in case of string " +
					"resources updates. Use ResolvableString to provide strings to notifications. " +
					"NotificationString will be removed in next minor release.",
			level = DeprecationLevel.ERROR,
			replaceWith = ReplaceWith(
				expression = "ResolvableString.raw(value)",
				imports = ["ru.solrudev.ackpine.resources.ResolvableString"]
			)
		)
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
		@Suppress("DEPRECATION_ERROR")
		@Deprecated(
			message = "This class cannot provide persistence stability of string resources in case of string " +
					"resources updates. Subclass ResolvableString.Resource to provide strings to notifications. " +
					"NotificationString will be removed in next minor release.",
			level = DeprecationLevel.ERROR,
			replaceWith = ReplaceWith(
				expression = "class StringResource(vararg args: Serializable) : ResolvableString.Resource(*args) { \n" +
						"\toverride fun stringId() = stringId\n\tprivate companion object {\n" +
						"\t\tprivate const val serialVersionUID = PUT_STRING_RESOURCE_SERIAL_VERSION_UID_HERE\n" +
						"\t}\n}",
				imports = ["ru.solrudev.ackpine.resources.ResolvableString"]
			)
		)
		public fun resource(@StringRes stringId: Int, vararg args: Serializable): NotificationString {
			return Resource(stringId, args)
		}
	}
}

@Suppress("DEPRECATION_ERROR")
private data object Default : NotificationString {
	private const val serialVersionUID = 809543744617543082L
	override fun resolve(context: Context): String = ""
}

@Suppress("DEPRECATION_ERROR")
private data object Empty : NotificationString {
	private const val serialVersionUID: Long = 5194188194930148316L
	override fun resolve(context: Context): String = ""
}

@Suppress("DEPRECATION_ERROR")
private data class Raw(val value: String) : NotificationString {
	override fun resolve(context: Context): String = value

	private companion object {
		private const val serialVersionUID: Long = -6824736411987160679L
	}
}

@Suppress("DEPRECATION_ERROR")
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
		return args.contentEquals(other.args)
	}

	override fun hashCode(): Int {
		var result = stringId
		result = 31 * result + args.contentHashCode()
		return result
	}

	private companion object {
		private const val serialVersionUID: Long = -7822872422889864805L
	}
}