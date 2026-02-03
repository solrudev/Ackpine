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

package ru.solrudev.ackpine.uninstaller

import androidx.concurrent.futures.await
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.uninstaller.parameters.UninstallParameters
import ru.solrudev.ackpine.uninstaller.parameters.UninstallParametersDsl
import java.util.UUID

/**
 * Creates an uninstall session. The returned session is in [pending][Session.State.Pending] state.
 *
 * @see PackageUninstaller.createSession
 * @param packageName name of the package to be uninstalled.
 * @param configure configures [uninstall session][UninstallParametersDsl].
 * @return [Session]
 */
public inline fun PackageUninstaller.createSession(
	packageName: String,
	configure: UninstallParametersDsl.() -> Unit = {}
): Session<UninstallFailure> {
	return createSession(UninstallParameters(packageName, configure))
}

/**
 * A suspending variant of [PackageUninstaller.getSessionAsync].
 * @return [Session] or `null` if not found.
 */
public suspend fun PackageUninstaller.getSession(sessionId: UUID): Session<UninstallFailure>? {
	return getSessionAsync(sessionId).await()
}

/**
 * A suspending variant of [PackageUninstaller.getSessionsAsync].
 * @return [Session] or `null` if not found.
 */
public suspend fun PackageUninstaller.getSessions(): List<Session<UninstallFailure>> {
	return getSessionsAsync().await()
}

/**
 * A suspending variant of [PackageUninstaller.getActiveSessionsAsync].
 * @return [Session] or `null` if not found.
 */
public suspend fun PackageUninstaller.getActiveSessions(): List<Session<UninstallFailure>> {
	return getActiveSessionsAsync().await()
}