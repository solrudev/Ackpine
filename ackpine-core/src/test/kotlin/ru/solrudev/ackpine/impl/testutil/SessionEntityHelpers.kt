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

import ru.solrudev.ackpine.impl.database.model.InstallConstraintsEntity
import ru.solrudev.ackpine.impl.database.model.InstallModeEntity
import ru.solrudev.ackpine.impl.database.model.InstallPreapprovalEntity
import ru.solrudev.ackpine.impl.database.model.PluginEntity
import ru.solrudev.ackpine.impl.database.model.SessionEntity
import ru.solrudev.ackpine.installer.parameters.InstallerType
import ru.solrudev.ackpine.resources.ResolvableString
import ru.solrudev.ackpine.session.Failure
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.session.parameters.Confirmation
import ru.solrudev.ackpine.session.parameters.DrawableId
import ru.solrudev.ackpine.uninstaller.parameters.UninstallerType

internal fun createBaseSessionEntity(
	id: String,
	type: SessionEntity.Type,
	state: SessionEntity.State,
	confirmation: Confirmation = Confirmation.DEFERRED,
	notificationTitle: ResolvableString = ResolvableString.raw("title"),
	notificationText: ResolvableString = ResolvableString.raw("text"),
	notificationIcon: DrawableId = TestDrawableId,
	requireUserAction: Boolean = true
) = SessionEntity(
	id, type, state, confirmation, notificationTitle,
	notificationText, notificationIcon, requireUserAction
)

internal fun createInstallSessionEntity(
	id: String,
	state: SessionEntity.State,
	installerType: InstallerType,
	uris: List<String>,
	notificationId: Int = 1,
	plugins: List<PluginEntity> = emptyList(),
	confirmation: Confirmation = Confirmation.DEFERRED,
	requireUserAction: Boolean = true,
	name: String? = null,
	installMode: InstallModeEntity? = null,
	packageName: String? = null,
	preapproval: InstallPreapprovalEntity? = null,
	constraints: InstallConstraintsEntity? = null,
	lastUpdateTimestamp: Long? = null,
	nativeSessionId: Int = -1,
	wasConfirmationLaunched: Boolean = false
) = SessionEntity.InstallSession(
	session = createBaseSessionEntity(
		id = id,
		type = SessionEntity.Type.INSTALL,
		state = state,
		confirmation = confirmation,
		requireUserAction = requireUserAction
	),
	installerType, uris, plugins, name, notificationId, installMode,
	packageName, lastUpdateTimestamp, preapproval, constraints,
	requestUpdateOwnership = null,
	packageSource = null,
	nativeSessionId, wasConfirmationLaunched
)

internal fun createUninstallSessionEntity(
	id: String,
	state: SessionEntity.State,
	uninstallerType: UninstallerType,
	packageName: String,
	notificationId: Int = 1,
	plugins: List<PluginEntity> = emptyList(),
	confirmation: Confirmation = Confirmation.DEFERRED
) = SessionEntity.UninstallSession(
	session = createBaseSessionEntity(
		id,
		type = SessionEntity.Type.UNINSTALL,
		state,
		confirmation
	),
	packageName,
	uninstallerType,
	notificationId,
	plugins
)

internal fun <F : Failure> SessionEntity.State.toSessionState(
	failureFactory: () -> F
): Session.State<F> = when (this) {
	SessionEntity.State.PENDING -> Session.State.Pending
	SessionEntity.State.ACTIVE -> Session.State.Active
	SessionEntity.State.AWAITING -> Session.State.Awaiting
	SessionEntity.State.COMMITTED -> Session.State.Committed
	SessionEntity.State.CANCELLED -> Session.State.Cancelled
	SessionEntity.State.SUCCEEDED -> Session.State.Succeeded
	SessionEntity.State.FAILED -> Session.State.Failed(failureFactory())
}