/*
 * Copyright (C) 2024 Ilya Fomichev
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

package ru.solrudev.ackpine.impl.database.dao

import androidx.annotation.RestrictTo
import androidx.room.Dao
import androidx.room.Query

@RestrictTo(RestrictTo.Scope.LIBRARY)
@Dao
internal interface InstallPreapprovalDao {

	@Query(
		"""
		UPDATE OR IGNORE sessions_install_preapproval
		SET is_activating = 1, is_active = 0
		WHERE session_id = :sessionId AND is_preapproved = 0
		"""
	)
	fun setActivating(sessionId: String): Int

	@Query(
		"""
		UPDATE OR IGNORE sessions_install_preapproval
		SET is_activating = 0, is_active = 1
		WHERE session_id = :sessionId AND is_activating = 1 AND is_preapproved = 0
		"""
	)
	fun setActive(sessionId: String): Int

	@Query(
		"""
		UPDATE OR IGNORE sessions_install_preapproval
		SET is_activating = 0,
		    is_active = 0,
		    is_preapproved = CASE WHEN :isPreapproved THEN 1 ELSE is_preapproved END
		WHERE session_id = :sessionId AND (is_active = 1 OR is_activating = 1)
		"""
	)
	fun consumeActive(sessionId: String, isPreapproved: Boolean): Int

	@Query(
		"""
        UPDATE OR IGNORE sessions_install_preapproval
        SET is_activating = 0,
            is_active = 0,
            is_preapproved = 0
        WHERE session_id = :sessionId
        """
	)
	fun reset(sessionId: String): Int
}