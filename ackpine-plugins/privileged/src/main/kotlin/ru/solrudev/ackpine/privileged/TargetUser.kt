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

import ru.solrudev.ackpine.privileged.TargetUser.Companion.CURRENT

/**
 * An Android user targeted by a privileged install or uninstall session.
 *
 * [userId] is an Android user ID, such as `0` for the system user, and not an application UID.
 *
 * @throws IllegalArgumentException if [userId] is negative and does not represent [CURRENT].
 */
public data class TargetUser(public val userId: Int) {

	init {
		require(userId >= 0 || userId == CURRENT_USER_ID) {
			"userId must be non-negative or represent the current Android user, but was $userId"
		}
	}

	public companion object {

		/**
		 * The Android user current when a session is bound to the package installer service.
		 */
		@JvmField
		public val CURRENT: TargetUser = TargetUser(CURRENT_USER_ID)

		private const val CURRENT_USER_ID = -2
	}
}