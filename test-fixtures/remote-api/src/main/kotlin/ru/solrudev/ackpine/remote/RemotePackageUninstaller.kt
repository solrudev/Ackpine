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

package ru.solrudev.ackpine.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.solrudev.ackpine.remote.dsl.RemoteUninstallParameters
import ru.solrudev.ackpine.remote.dsl.RemoteUninstallParametersDsl
import ru.solrudev.ackpine.remote.dsl.RemoteUninstallParametersDslBuilder
import ru.solrudev.ackpine.resources.ResolvableString
import ru.solrudev.ackpine.session.parameters.Confirmation
import ru.solrudev.ackpine.session.parameters.notification
import ru.solrudev.ackpine.uninstaller.PackageUninstaller
import ru.solrudev.ackpine.uninstaller.createSession
import ru.solrudev.ackpine.uninstaller.parameters.UninstallerType
import java.util.UUID

/**
 * A [PackageUninstaller] bridge for IPC.
 */
public class RemotePackageUninstaller internal constructor(private val uninstaller: IPackageUninstaller) {

	/**
	 * Creates an uninstall [RemoteSession] configured via [DSL][RemoteUninstallParametersDsl]. The returned session is
	 * in [pending][RemoteSession.State.Pending] state.
	 */
	public inline fun createSession(
		packageName: String,
		configure: RemoteUninstallParametersDsl.() -> Unit = {}
	): RemoteSession {
		val parameters = RemoteUninstallParametersDslBuilder(packageName).apply(configure).build()
		return createSession(parameters)
	}

	/**
	 * Returns an uninstall [RemoteSession] which matches the provided [sessionId], or `null` if not found.
	 */
	public suspend fun getSession(sessionId: UUID): RemoteSession? = withContext(Dispatchers.IO) {
		uninstaller.getSession(sessionId.toString())?.let(::RemoteSession)
	}

	@PublishedApi
	internal fun createSession(parameters: RemoteUninstallParameters): RemoteSession = with(parameters) {
		val session = uninstaller.createSession(
			uninstallParameters.uninstallerType.ordinal,
			uninstallParameters.packageName,
			uninstallParameters.confirmation.ordinal,
			notificationData.title,
			notificationData.contentText
		)
		RemoteSession(session)
	}
}

internal class RemotePackageUninstallerImpl(private val uninstaller: PackageUninstaller) : IPackageUninstaller.Stub() {

	override fun createSession(
		type: Int,
		packageName: String,
		confirmation: Int,
		notificationTitle: String,
		notificationText: String
	): ISession {
		val session = uninstaller.createSession(packageName) {
			uninstallerType = UninstallerType.entries[type]
			this.confirmation = Confirmation.entries[confirmation]
			notification {
				if (notificationTitle.isNotEmpty()) {
					title = ResolvableString.raw(notificationTitle)
				}
				if (notificationText.isNotEmpty()) {
					contentText = ResolvableString.raw(notificationText)
				}
			}
		}
		return RemoteSessionImpl(session)
	}

	override fun getSession(id: String): ISession? {
		val session = uninstaller.getSessionAsync(UUID.fromString(id)).get() ?: return null
		return RemoteSessionImpl(session)
	}
}