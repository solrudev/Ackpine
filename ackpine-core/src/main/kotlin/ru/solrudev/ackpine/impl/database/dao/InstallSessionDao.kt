package ru.solrudev.ackpine.impl.database.dao

import androidx.annotation.RestrictTo
import androidx.room.*
import ru.solrudev.ackpine.impl.database.AckpineDatabase
import ru.solrudev.ackpine.impl.database.model.InstallUriEntity
import ru.solrudev.ackpine.impl.database.model.SessionEntity
import ru.solrudev.ackpine.installer.InstallFailure
import ru.solrudev.ackpine.installer.parameters.InstallerType

@RestrictTo(RestrictTo.Scope.LIBRARY)
@Dao
internal abstract class InstallSessionDao(private val database: AckpineDatabase) : SessionFailureDao<InstallFailure> {

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
		insertUris(session.uris.map { uri ->
			InstallUriEntity(sessionId = session.session.id, uri = uri)
		})
		database.sessionProgressDao().initProgress(session.session.id)
	}

	@Transaction
	@Query("SELECT * FROM sessions WHERE id = :id")
	abstract fun getInstallSession(id: String): SessionEntity.InstallSession?

	@Query("INSERT OR IGNORE INTO sessions_install_failures(session_id, failure) VALUES (:id, :failure)")
	abstract fun insertInstallFailure(id: String, failure: InstallFailure)

	@Query("INSERT OR IGNORE INTO sessions_installer_types(session_id, installer_type) VALUES (:id, :installerType)")
	abstract fun insertInstallerType(id: String, installerType: InstallerType)

	@Insert(onConflict = OnConflictStrategy.IGNORE)
	abstract fun insertUris(uris: List<InstallUriEntity>)
}