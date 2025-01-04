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
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import ru.solrudev.ackpine.impl.database.AckpineDatabase
import ru.solrudev.ackpine.impl.database.model.InstallConstraintsEntity
import ru.solrudev.ackpine.impl.database.model.InstallModeEntity
import ru.solrudev.ackpine.impl.database.model.InstallPreapprovalEntity
import ru.solrudev.ackpine.impl.database.model.InstallUriEntity
import ru.solrudev.ackpine.impl.database.model.SessionEntity
import ru.solrudev.ackpine.installer.InstallFailure
import ru.solrudev.ackpine.installer.parameters.InstallerType
import ru.solrudev.ackpine.installer.parameters.PackageSource

@RestrictTo(RestrictTo.Scope.LIBRARY)
@Dao
internal abstract class InstallSessionDao protected constructor(private val database: AckpineDatabase)
	: SessionFailureDao<InstallFailure> {

	@Query("SELECT failure FROM sessions_install_failures WHERE session_id = :id")
	abstract override fun getFailure(id: String): InstallFailure?

	@Transaction
	override fun setFailure(id: String, failure: InstallFailure) {
		database.sessionDao().updateSessionState(id, SessionEntity.State.FAILED)
		insertInstallFailure(id, failure)
	}

	@Transaction
	open fun insertInstallSession(session: SessionEntity.InstallSession) {
		database.sessionDao().insertSession(session.session)
		insertInstallerType(session.session.id, session.installerType)
		if (session.installMode != null) {
			insertInstallMode(session.installMode)
		}
		insertUris(session.uris.map { uri ->
			InstallUriEntity(sessionId = session.session.id, uri = uri)
		})
		database.sessionProgressDao().initProgress(session.session.id)
		database.notificationIdDao().initNotificationId(session.session.id, session.notificationId!!)
		if (!session.name.isNullOrEmpty()) {
			database.sessionNameDao().setSessionName(session.session.id, session.name)
		}
		if (session.packageName != null) {
			insertPackageName(session.session.id, session.packageName)
		}
		if (session.preapproval != null) {
			insertInstallPreapproval(session.preapproval)
		}
		if (session.constraints != null) {
			insertInstallConstraints(session.constraints)
		}
		if (session.requestUpdateOwnership != null) {
			insertRequestUpdateOwnership(session.session.id, session.requestUpdateOwnership)
		}
		if (session.packageSource != null) {
			insertPackageSource(session.session.id, session.packageSource)
		}
	}

	@Transaction
	@Query("SELECT * FROM sessions WHERE id = :id AND type = 'INSTALL'")
	abstract fun getInstallSession(id: String): SessionEntity.InstallSession?

	@Transaction
	@Query("SELECT * FROM sessions WHERE type = 'INSTALL'")
	abstract fun getInstallSessions(): List<SessionEntity.InstallSession>

	@Transaction
	@Query("SELECT * FROM sessions WHERE state = 'COMMITTED' AND type = 'INSTALL' ORDER BY last_commit_timestamp DESC")
	abstract fun getCommittedInstallSessions(): List<SessionEntity.InstallSession>

	@Query("INSERT OR IGNORE INTO sessions_install_failures(session_id, failure) VALUES (:id, :failure)")
	protected abstract fun insertInstallFailure(id: String, failure: InstallFailure)

	@Query("INSERT OR IGNORE INTO sessions_installer_types(session_id, installer_type) VALUES (:id, :installerType)")
	protected abstract fun insertInstallerType(id: String, installerType: InstallerType)

	@Insert(onConflict = OnConflictStrategy.IGNORE)
	protected abstract fun insertInstallMode(installMode: InstallModeEntity)

	@Query("INSERT OR IGNORE INTO sessions_package_names(session_id, package_name) VALUES (:id, :packageName)")
	abstract fun insertPackageName(id: String, packageName: String)

	@Insert(onConflict = OnConflictStrategy.IGNORE)
	protected abstract fun insertInstallPreapproval(installPreapproval: InstallPreapprovalEntity)

	@Insert(onConflict = OnConflictStrategy.IGNORE)
	protected abstract fun insertInstallConstraints(installConstraints: InstallConstraintsEntity)

	@Query(
		"INSERT OR IGNORE INTO sessions_update_ownership(session_id, request_update_ownership) " +
				"VALUES (:id, :requestUpdateOwnership)"
	)
	protected abstract fun insertRequestUpdateOwnership(id: String, requestUpdateOwnership: Boolean)

	@Query("INSERT OR IGNORE INTO sessions_package_sources(session_id, package_source) VALUES (:id, :packageSource)")
	protected abstract fun insertPackageSource(id: String, packageSource: PackageSource)

	@Insert(onConflict = OnConflictStrategy.IGNORE)
	protected abstract fun insertUris(uris: List<InstallUriEntity>)
}