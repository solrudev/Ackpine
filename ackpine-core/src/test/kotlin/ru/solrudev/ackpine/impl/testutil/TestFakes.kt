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

import ru.solrudev.ackpine.impl.database.dao.LastUpdateTimestampDao
import ru.solrudev.ackpine.impl.database.dao.SessionFailureDao
import ru.solrudev.ackpine.impl.installer.session.PackageInstallerStatus
import ru.solrudev.ackpine.impl.installer.session.PreapprovalListener
import ru.solrudev.ackpine.installer.InstallFailure
import ru.solrudev.ackpine.session.Failure
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.session.parameters.DrawableId
import java.util.UUID
import java.util.concurrent.Executor

internal object ImmediateExecutor : Executor {
	override fun execute(command: Runnable) {
		command.run()
	}
}

internal sealed interface TestFailure : Failure {
	data class Exceptional(override val exception: Exception) : TestFailure, Failure.Exceptional
	data class Aborted(val message: String) : TestFailure
}

internal class TestSessionFailureDao<F : Failure> : SessionFailureDao<F> {

	private val failures = mutableMapOf<String, F>()

	override fun getFailure(id: String) = failures[id]

	override fun setFailure(id: String, failure: F) {
		failures[id] = failure
	}
}

internal object DummyLastUpdateTimestampDao : LastUpdateTimestampDao {

	override fun setLastUpdateTimestamp(sessionId: String, packageName: String, lastUpdateTimestamp: Long) { // no-op
	}

	override fun setLastUpdateTimestamp(sessionId: String, lastUpdateTimestamp: Long) { // no-op
	}
}

internal object TestDrawableId : DrawableId {
	override fun drawableId(): Int = android.R.drawable.ic_dialog_alert

	@Suppress("Unused")
	private const val serialVersionUID = -2471950954120221872L
}

internal open class TestPreapprovalSession(
	id: UUID,
	initialState: Session.State<InstallFailure> = Session.State.Pending,
	exceptionalFailureFactory: ((Exception) -> InstallFailure)? = null
) : TestCompletableProgressSession<InstallFailure>(
	id,
	initialState,
	exceptionalFailureFactory
), PreapprovalListener {

	var preapprovalStarted = false
	var preapprovalSucceeded = false
	var preapprovalFailure: PreapprovalFailure? = null

	override fun onPreapprovalStarted() {
		preapprovalStarted = true
	}

	override fun onPreapprovalSucceeded() {
		preapprovalSucceeded = true
	}

	override fun onPreapprovalFailed(status: PackageInstallerStatus?, publicFailure: InstallFailure) {
		preapprovalFailure = PreapprovalFailure(status, publicFailure)
	}

	data class PreapprovalFailure(val status: PackageInstallerStatus?, val failure: InstallFailure)
}