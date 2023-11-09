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

package ru.solrudev.ackpine.installer

import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.concurrent.futures.await
import ru.solrudev.ackpine.exceptions.SplitPackagesNotSupportedException
import ru.solrudev.ackpine.installer.parameters.InstallParameters
import ru.solrudev.ackpine.installer.parameters.InstallParametersDsl
import ru.solrudev.ackpine.session.ProgressSession
import ru.solrudev.ackpine.session.Session
import java.util.UUID

/**
 * Creates an install session. The returned session is in [pending][Session.State.Pending] state.
 *
 * Split packages are not supported on API levels < 21.
 * Attempting to add additional APKs on these API levels may produce [SplitPackagesNotSupportedException].
 *
 * @see PackageInstaller.createSession
 * @param baseApk [URI][Uri] of base APK.
 * @param configure configures [install session][InstallParametersDsl].
 * @return [ProgressSession]
 */
public inline fun PackageInstaller.createSession(
	baseApk: Uri,
	configure: InstallParametersDsl.() -> Unit = {}
): ProgressSession<InstallFailure> {
	return createSession(InstallParameters(baseApk, configure))
}

/**
 * Creates an install session. The returned session is in [pending][Session.State.Pending] state.
 *
 * Split packages are not supported on API levels < 21.
 * Attempting to add additional APKs on these API levels may produce [SplitPackagesNotSupportedException].
 *
 * @see PackageInstaller.createSession
 * @param apks [URIs][Uri] of split APKs.
 * @param configure configures [install session][InstallParametersDsl].
 * @return [ProgressSession]
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public inline fun PackageInstaller.createSession(
	apks: Iterable<Uri>,
	configure: InstallParametersDsl.() -> Unit = {}
): ProgressSession<InstallFailure> {
	return createSession(InstallParameters(apks, configure))
}

/**
 * A suspending variant of [PackageInstaller.getSessionAsync].
 * @return [ProgressSession] or `null` if not found.
 */
public suspend fun PackageInstaller.getSession(sessionId: UUID): ProgressSession<InstallFailure>? {
	return getSessionAsync(sessionId).await()
}

/**
 * A suspending variant of [PackageInstaller.getSessionsAsync].
 * @return List of [ProgressSessions][ProgressSession].
 */
public suspend fun PackageInstaller.getSessions(): List<ProgressSession<InstallFailure>> {
	return getSessionsAsync().await()
}

/**
 * A suspending variant of [PackageInstaller.getActiveSessionsAsync].
 * @return List of [ProgressSessions][ProgressSession].
 */
public suspend fun PackageInstaller.getActiveSessions(): List<ProgressSession<InstallFailure>> {
	return getActiveSessionsAsync().await()
}