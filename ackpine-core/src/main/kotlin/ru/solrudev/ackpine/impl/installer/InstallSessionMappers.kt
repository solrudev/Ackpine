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

package ru.solrudev.ackpine.impl.installer

import androidx.core.net.toUri
import ru.solrudev.ackpine.impl.database.dao.InstallSessionDao
import ru.solrudev.ackpine.impl.database.dao.SessionProgressDao
import ru.solrudev.ackpine.impl.database.model.InstallModeEntity
import ru.solrudev.ackpine.impl.database.model.SessionEntity
import ru.solrudev.ackpine.impl.session.toSessionState
import ru.solrudev.ackpine.installer.InstallFailure
import ru.solrudev.ackpine.installer.parameters.InstallConstraints
import ru.solrudev.ackpine.installer.parameters.InstallMode
import ru.solrudev.ackpine.installer.parameters.InstallPreapproval
import ru.solrudev.ackpine.session.Progress
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.session.parameters.NotificationData

@JvmSynthetic
internal fun SessionEntity.InstallSession.getState(
	installSessionDao: InstallSessionDao
): Session.State<InstallFailure> {
	return session.state.toSessionState(session.id, installSessionDao)
}

@JvmSynthetic
internal fun SessionEntity.InstallSession.getProgress(sessionProgressDao: SessionProgressDao): Progress {
	return sessionProgressDao.getProgress(session.id) ?: Progress()
}

@JvmSynthetic
internal fun SessionEntity.InstallSession.getNotificationData() = NotificationData.Builder()
	.setTitle(session.notificationTitle)
	.setContentText(session.notificationText)
	.setIcon(session.notificationIcon)
	.build()

@JvmSynthetic
internal fun SessionEntity.InstallSession.getInstallMode(): InstallMode {
	return when (installMode?.installMode) {
		null -> InstallMode.Full
		InstallModeEntity.InstallMode.FULL -> InstallMode.Full
		InstallModeEntity.InstallMode.INHERIT_EXISTING -> InstallMode.InheritExisting(
			requireNotNull(packageName) {
				"Package name was null when install mode is INHERIT_EXISTING"
			},
			installMode.dontKillApp
		)
	}
}

@JvmSynthetic
internal fun SessionEntity.InstallSession.getPreapproval(): InstallPreapproval {
	if (preapproval == null) {
		return InstallPreapproval.NONE
	}
	return InstallPreapproval.Builder(
		preapproval.packageName,
		preapproval.label,
		preapproval.locale
	)
		.setIcon(preapproval.icon.toUri())
		.build()
}

@JvmSynthetic
internal fun SessionEntity.InstallSession.getConstraints(): InstallConstraints {
	if (constraints == null) {
		return InstallConstraints.NONE
	}
	return InstallConstraints.Builder(constraints.timeoutMillis)
		.setAppNotForegroundRequired(constraints.isAppNotForegroundRequired)
		.setAppNotInteractingRequired(constraints.isAppNotInteractingRequired)
		.setAppNotTopVisibleRequired(constraints.isAppNotTopVisibleRequired)
		.setDeviceIdleRequired(constraints.isDeviceIdleRequired)
		.setNotInCallRequired(constraints.isNotInCallRequired)
		.setTimeoutStrategy(constraints.timeoutStrategy)
		.build()
}