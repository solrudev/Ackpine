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

package ru.solrudev.ackpine.privileged

import ru.solrudev.ackpine.plugability.AckpinePlugin

/**
 * Shared uninstall parameters for privileged Ackpine plugins.
 */
public abstract class PrivilegedUninstallParameters protected constructor(

	/**
	 * Flag parameter to indicate that you don't want to delete the package's data directory.
	 */
	public val keepData: Boolean,

	/**
	 * Flag parameter to indicate that you want the package deleted for all users.
	 */
	public val allUsers: Boolean
) : AckpinePlugin.Parameters {

	/**
	 * Returns the simple class name used in [toString].
	 */
	protected abstract fun getName(): String

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false
		other as PrivilegedUninstallParameters
		if (keepData != other.keepData) return false
		if (allUsers != other.allUsers) return false
		return true
	}

	override fun hashCode(): Int {
		var result = keepData.hashCode()
		result = 31 * result + allUsers.hashCode()
		return result
	}

	override fun toString(): String = "${getName()}(" +
			"keepData=$keepData, " +
			"allUsers=$allUsers" +
			")"

	/**
	 * Base builder for privileged uninstall parameters.
	 */
	public abstract class Builder<Params : PrivilegedUninstallParameters, Self : Builder<Params, Self>> {

		/**
		 * Flag parameter to indicate that you don't want to delete the package's data directory.
		 */
		public var keepData: Boolean = false
			protected set

		/**
		 * Flag parameter to indicate that you want the package deleted for all users.
		 */
		public var allUsers: Boolean = false
			protected set

		/**
		 * Sets [PrivilegedUninstallParameters.keepData].
		 */
		public open fun setKeepData(value: Boolean): Self = self().apply {
			keepData = value
		}

		/**
		 * Sets [PrivilegedUninstallParameters.allUsers].
		 */
		public open fun setAllUsers(value: Boolean): Self = self().apply {
			allUsers = value
		}

		/**
		 * Constructs a new instance of privileged uninstall parameters.
		 */
		public abstract fun build(): Params

		@Suppress("UNCHECKED_CAST")
		private fun self() = this as Self
	}
}