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

package ru.solrudev.ackpine.impl.testutil

import android.content.IntentSender
import android.content.pm.PackageInstaller
import android.os.Handler
import org.robolectric.util.ReflectionHelpers
import ru.solrudev.ackpine.impl.database.dao.InstallConstraintsDao
import ru.solrudev.ackpine.impl.database.dao.InstallPreapprovalDao
import ru.solrudev.ackpine.impl.database.dao.NativeSessionIdDao
import ru.solrudev.ackpine.impl.database.dao.SessionDao
import ru.solrudev.ackpine.impl.database.dao.SessionProgressDao
import ru.solrudev.ackpine.impl.database.model.SessionEntity
import ru.solrudev.ackpine.impl.services.PackageInstallerService
import ru.solrudev.ackpine.plugability.AckpinePlugin
import ru.solrudev.ackpine.session.Progress
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.util.UUID

internal class RecordingSessionDao : SessionDao {

	private val _insertedSessions = mutableListOf<SessionEntity>()
	val insertedSessions: List<SessionEntity> = _insertedSessions

	private val _stateUpdates = mutableListOf<SessionStateUpdate>()
	val stateUpdates: List<SessionStateUpdate> = _stateUpdates

	private val _lastLaunchUpdates = mutableListOf<LastLaunchUpdate>()
	val lastLaunchUpdates: List<LastLaunchUpdate> = _lastLaunchUpdates

	private val _lastCommitUpdates = mutableListOf<LastCommitUpdate>()
	val lastCommitUpdates: List<LastCommitUpdate> = _lastCommitUpdates

	override fun insertSession(session: SessionEntity) {
		_insertedSessions += session
	}

	override fun updateSessionState(id: String, state: SessionEntity.State) {
		_stateUpdates += SessionStateUpdate(id, state)
	}

	override fun updateLastLaunchTimestamp(id: String, lastLaunchTimestamp: Long) {
		_lastLaunchUpdates += LastLaunchUpdate(id, lastLaunchTimestamp)
	}

	override fun updateLastCommitTimestamp(id: String, lastCommitTimestamp: Long) {
		_lastCommitUpdates += LastCommitUpdate(id, lastCommitTimestamp)
	}
}

internal class RecordingSessionProgressDao : SessionProgressDao {

	private val _progressUpdates = mutableMapOf<String, MutableList<Progress>>()
	val progressUpdates: Map<String, List<Progress>> = _progressUpdates

	override fun getProgress(id: String) = _progressUpdates[id]?.lastOrNull()

	override fun initProgress(id: String) {
		_progressUpdates[id] = mutableListOf()
	}

	override fun updateProgress(id: String, progress: Int, max: Int) {
		val updates = _progressUpdates[id] ?: mutableListOf<Progress>().also { _progressUpdates[id] = it }
		updates += Progress(progress, max)
	}
}

internal class RecordingNativeSessionIdDao : NativeSessionIdDao {

	private val _nativeSessionIds = mutableMapOf<String, Int>()
	val nativeSessionIds: Map<String, Int> = _nativeSessionIds

	private val _removed = mutableListOf<String>()
	val removed: List<String> = _removed

	override fun setNativeSessionId(sessionId: String, nativeSessionId: Int) {
		_nativeSessionIds[sessionId] = nativeSessionId
	}

	override fun removeNativeSessionId(sessionId: String) {
		_removed += sessionId
	}
}

internal class RecordingInstallPreapprovalDao : InstallPreapprovalDao {

	private val _preapprovedSessions = mutableListOf<String>()
	val preapprovedSessions: List<String> = _preapprovedSessions

	override fun setPreapproved(sessionId: String) {
		_preapprovedSessions += sessionId
	}
}

internal class RecordingInstallConstraintsDao : InstallConstraintsDao {

	private val _commitAttemptsUpdates = mutableListOf<CommitAttemptsUpdate>()
	val commitAttemptsUpdates: List<CommitAttemptsUpdate> = _commitAttemptsUpdates

	override fun setCommitAttemptsCount(sessionId: String, commitAttemptsCount: Int) {
		_commitAttemptsUpdates += CommitAttemptsUpdate(sessionId, commitAttemptsCount)
	}
}

internal class RecordingPackageInstallerService : PackageInstallerService {

	override val uid = 10000
	val session = RecordingSession()

	private val _uninstallCalls = mutableListOf<UninstallCall>()
	val uninstallCalls: List<UninstallCall> = _uninstallCalls

	private val _abandonedSessions = mutableListOf<Int>()
	val abandonedSessions: List<Int> = _abandonedSessions

	private val _commitAfterConstraintsCalls = mutableListOf<CommitAfterConstraintsCall>()
	val commitAfterConstraintsCalls: List<CommitAfterConstraintsCall> = _commitAfterConstraintsCalls

	private val _registeredCallbacks = mutableListOf<PackageInstaller.SessionCallback>()
	val registeredCallbacks: List<PackageInstaller.SessionCallback> = _registeredCallbacks

	private val _unregisteredCallbacks = mutableListOf<PackageInstaller.SessionCallback>()
	val unregisteredCallbacks: List<PackageInstaller.SessionCallback> = _unregisteredCallbacks

	private val _createdSessions = mutableListOf<CreatedSessionRecord>()
	val createdSessions: List<CreatedSessionRecord> = _createdSessions

	private val createdSessionIds = mutableSetOf<Int>()
	private var nextSessionId = 1

	override fun applyParameters(sessionId: UUID, parameters: AckpinePlugin.Parameters) { // no-op
	}

	override fun createSession(params: PackageInstaller.SessionParams, ackpineSessionId: UUID): Int {
		_createdSessions += CreatedSessionRecord(params, ackpineSessionId)
		val sessionId = nextSessionId++
		createdSessionIds += sessionId
		return sessionId
	}

	override fun openSession(sessionId: Int) = session

	override fun getSessionInfo(sessionId: Int): PackageInstaller.SessionInfo? {
		if (sessionId !in createdSessionIds) {
			return null
		}
		return ReflectionHelpers.callConstructor(PackageInstaller.SessionInfo::class.java)
	}

	override fun commitSessionAfterInstallConstraintsAreMet(
		sessionId: Int,
		statusReceiver: IntentSender,
		constraints: PackageInstaller.InstallConstraints,
		timeoutMillis: Long
	) {
		_commitAfterConstraintsCalls += CommitAfterConstraintsCall(
			sessionId,
			statusReceiver,
			constraints,
			timeoutMillis
		)
	}

	override fun registerSessionCallback(callback: PackageInstaller.SessionCallback, handler: Handler) {
		_registeredCallbacks += callback
	}

	override fun unregisterSessionCallback(callback: PackageInstaller.SessionCallback) {
		_unregisteredCallbacks += callback
	}

	override fun abandonSession(sessionId: Int) {
		_abandonedSessions += sessionId
	}

	override fun uninstall(packageName: String, statusReceiver: IntentSender, ackpineSessionId: UUID) {
		_uninstallCalls += UninstallCall(packageName, statusReceiver, ackpineSessionId)
	}

	data class CommitAfterConstraintsCall(
		val sessionId: Int,
		val statusReceiver: IntentSender,
		val constraints: PackageInstaller.InstallConstraints,
		val timeoutMillis: Long
	)

	data class UninstallCall(
		val packageName: String,
		val statusReceiver: IntentSender,
		val ackpineSessionId: UUID
	)

	class RecordingSession : PackageInstallerService.Session {

		private val _writes = mutableMapOf<String, ByteArray>()
		val writes: Map<String, ByteArray> = _writes

		private val _commits = mutableListOf<IntentSender>()
		val commits: List<IntentSender> = _commits

		private val _preapprovalRequests = mutableListOf<PreapprovalRequest>()
		val preapprovalRequests: List<PreapprovalRequest> = _preapprovalRequests

		override fun openWrite(name: String, offsetBytes: Long, lengthBytes: Long): OutputStream {
			return object : ByteArrayOutputStream() {
				override fun close() {
					_writes[name] = toByteArray()
				}
			}
		}

		override fun fsync(out: OutputStream) { // no-op
		}

		override fun setStagingProgress(progress: Float) { // no-op
		}

		override fun commit(statusReceiver: IntentSender) {
			_commits += statusReceiver
		}

		override fun requestUserPreapproval(
			details: PackageInstaller.PreapprovalDetails,
			statusReceiver: IntentSender
		) {
			_preapprovalRequests += PreapprovalRequest(details, statusReceiver)
		}

		override fun close() { // no-op
		}

		data class PreapprovalRequest(
			val details: PackageInstaller.PreapprovalDetails,
			val statusReceiver: IntentSender
		)
	}
}

internal data class SessionStateUpdate(
	val sessionId: String,
	val state: SessionEntity.State
)

internal data class LastLaunchUpdate(
	val sessionId: String,
	val timestamp: Long
)

internal data class LastCommitUpdate(
	val sessionId: String,
	val timestamp: Long
)

internal data class CommitAttemptsUpdate(
	val sessionId: String,
	val commitAttemptsCount: Int
)

internal data class CreatedSessionRecord(
	val params: PackageInstaller.SessionParams,
	val ackpineSessionId: UUID
)