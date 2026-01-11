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

@file:Suppress("Unused", "ConstPropertyName")

package ru.solrudev.ackpine.resources

import android.content.Context
import androidx.annotation.StringRes
import ru.solrudev.ackpine.resources.ResolvableString.Resource
import java.io.Serializable

/**
 * String which can be resolved at use site.
 */
public sealed interface ResolvableString : Serializable {

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

	/**
	 * Static factories for [ResolvableString].
	 */
	public companion object {

		/**
		 * Returns an empty [ResolvableString].
		 */
		@JvmStatic
		public fun empty(): ResolvableString = Empty

		/**
		 * Creates [ResolvableString] with a hardcoded value.
		 */
		@JvmStatic
		public fun raw(value: String): ResolvableString {
			if (value.isEmpty()) {
				return Empty
			}
			return Raw(value)
		}

		/**
		 * Creates a transient instance of [ResolvableString.Resource], which is a [ResolvableString] backed by
		 * Android resource string with optional [arguments][args]. Arguments can be
		 * [ResolvableStrings][ResolvableString] as well.
		 *
		 * This factory is meant to create only **transient** strings, i.e. not persisted in storage, because string IDs
		 * are not stable. For persisted strings [ResolvableString.Resource] should be explicitly subclassed. Example:
		 * ```
		 * object InstallMessageTitle : ResolvableString.Resource() {
		 *     override fun stringId() = R.string.install_message_title
		 *     private const val serialVersionUID = -1310602635578779088L
		 *     private fun readResolve(): Any = InstallMessageTitle
		 * }
		 *
		 * class InstallMessage(fileName: String) : ResolvableString.Resource(fileName) {
		 *     override fun stringId() = R.string.install_message
		 *     private companion object {
		 *         private const val serialVersionUID = 4749568844072243110L
		 *     }
		 * }
		 * ```
		 *
		 * @param stringId Android string resource ID
		 * @param args string format arguments
		 */
		@JvmStatic
		public fun transientResource(@StringRes stringId: Int, vararg args: Serializable): ResolvableString {
			return TransientResource(stringId, args)
		}
	}

	/**
	 * [ResolvableString] backed by Android resource string with optional [arguments][args]. Arguments can be
	 * [ResolvableStrings][ResolvableString] as well.
	 *
	 * Should be explicitly subclassed to ensure stable persistence, and `serialVersionUID` must be present. Example:
	 * ```
	 * object InstallMessageTitle : ResolvableString.Resource() {
	 *     override fun stringId() = R.string.install_message_title
	 *     private const val serialVersionUID = -1310602635578779088L
	 *     private fun readResolve(): Any = InstallMessageTitle
	 * }
	 *
	 * class InstallMessage(fileName: String) : ResolvableString.Resource(fileName) {
	 *     override fun stringId() = R.string.install_message
	 *     private companion object {
	 *         private const val serialVersionUID = 4749568844072243110L
	 *     }
	 * }
	 * ```
	 * For transient strings, i.e. not persisted in storage, you can use [ResolvableString.transientResource] factory.
	 *
	 * @param args string format arguments
	 */
	public abstract class Resource(private vararg val args: Serializable) : ResolvableString {

		/**
		 * Returns an Android string resource ID.
		 */
		@StringRes
		protected abstract fun stringId(): Int

		final override fun resolve(context: Context): String = context.getString(stringId(), *resolveArgs(context))

		private fun resolveArgs(context: Context): Array<Serializable> = args.map { argument ->
			if (argument is ResolvableString) {
				argument.resolve(context)
			} else {
				argument
			}
		}.toTypedArray()

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false
			other as Resource
			return args.contentEquals(other.args)
		}

		override fun hashCode(): Int {
			return args.contentHashCode()
		}

		private companion object {
			private const val serialVersionUID = -7766769726170724379L
		}
	}
}

private class TransientResource(
	@param:StringRes private val stringId: Int,
	private val args: Array<out Serializable>
) : Resource(stringId, args) {

	override fun stringId() = stringId

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false
		other as TransientResource
		if (stringId != other.stringId) return false
		if (!args.contentDeepEquals(other.args)) return false
		return true
	}

	override fun hashCode(): Int {
		var result = super.hashCode()
		result = 31 * result + stringId
		result = 31 * result + args.contentDeepHashCode()
		return result
	}

	private companion object {
		private const val serialVersionUID: Long = 2599070109225891907L
	}
}

private data object Empty : ResolvableString {
	private const val serialVersionUID = 5194188194930148316L
	override fun resolve(context: Context): String = ""
	private fun readResolve(): Any = Empty
}

private data class Raw(val value: String) : ResolvableString {
	override fun resolve(context: Context): String = value
	private companion object {
		private const val serialVersionUID = -6824736411987160679L
	}
}