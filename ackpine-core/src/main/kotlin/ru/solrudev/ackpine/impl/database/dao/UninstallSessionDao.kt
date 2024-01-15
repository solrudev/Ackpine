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

package ru.solrudev.ackpine.impl.database.dao

import androidx.annotation.RestrictTo
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import ru.solrudev.ackpine.impl.database.AckpineDatabase
import ru.solrudev.ackpine.impl.database.model.SessionEntity
import ru.solrudev.ackpine.uninstaller.UninstallFailure

@RestrictTo(RestrictTo.Scope.LIBRARY)
@Dao
internal abstract class UninstallSessionDao protected constructor(private val database: AckpineDatabase) :
	SessionFailureDao<UninstallFailure> {

	@Query("SELECT failure FROM sessions_uninstall_failures WHERE session_id = :id")
	abstract override fun getFailure(id: String): UninstallFailure?

	@Transaction
	override fun setFailure(id: String, failure: UninstallFailure) {
		database.sessionDao().updateSessionState(id, SessionEntity.State.FAILED)
		insertUninstallFailure(id, failure)
	}

	@Transaction
	open fun insertUninstallSession(session: SessionEntity.UninstallSession) {
		database.sessionDao().insertSession(session.session)
		insertPackageName(session.session.id, session.packageName)
		database.notificationIdDao().initNotificationId(session.session.id)
	}

	@Transaction
	@Query("SELECT * FROM sessions WHERE id = :id AND type = 'UNINSTALL'")
	abstract fun getUninstallSession(id: String): SessionEntity.UninstallSession?

	@Transaction
	@Query("SELECT * FROM sessions WHERE type = 'UNINSTALL'")
	abstract fun getUninstallSessions(): List<SessionEntity.UninstallSession>

	@Query("INSERT OR IGNORE INTO sessions_uninstall_failures(session_id, failure) VALUES (:id, :failure)")
	protected abstract fun insertUninstallFailure(id: String, failure: UninstallFailure)

	@Query("INSERT INTO sessions_package_names(session_id, package_name) VALUES (:id, :packageName)")
	protected abstract fun insertPackageName(id: String, packageName: String)
}