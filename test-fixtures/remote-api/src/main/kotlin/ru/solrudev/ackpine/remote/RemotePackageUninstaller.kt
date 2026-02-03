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

import ru.solrudev.ackpine.installer.parameters.InstallerType
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
	 * Creates an uninstall [RemoteSession] with [Confirmation.IMMEDIATE] option.
	 */
	public fun createImmediateSession(
		type: UninstallerType,
		packageName: String
	): RemoteSession {
		val session = uninstaller.createImmediateSession(type.name, packageName)
		return RemoteSession(session)
	}

	/**
	 * Creates an uninstall [RemoteSession] with [Confirmation.DEFERRED] option.
	 */
	public fun createDeferredSession(
		type: InstallerType,
		packageName: String,
		notificationTitle: String
	): RemoteSession {
		val session = uninstaller.createDeferredSession(type.name, packageName, notificationTitle)
		return RemoteSession(session)
	}

	/**
	 * Returns an uninstall [RemoteSession] which matches the provided [sessionId], or `null` if not found.
	 */
	public fun getSession(sessionId: UUID): RemoteSession? {
		val session = uninstaller.getSession(sessionId.toString()) ?: return null
		return RemoteSession(session)
	}
}

internal class RemotePackageUninstallerImpl(private val uninstaller: PackageUninstaller) : IPackageUninstaller.Stub() {

	override fun createImmediateSession(
		type: String,
		packageName: String
	): ISession {
		val session = uninstaller.createSession(packageName) {
			uninstallerType = UninstallerType.valueOf(type)
			confirmation = Confirmation.IMMEDIATE
		}
		return RemoteSessionImpl(session)
	}

	override fun createDeferredSession(
		type: String,
		packageName: String,
		notificationTitle: String
	): ISession {
		val session = uninstaller.createSession(packageName) {
			uninstallerType = UninstallerType.valueOf(type)
			confirmation = Confirmation.DEFERRED
			notification {
				title = ResolvableString.raw(notificationTitle)
			}
		}
		return RemoteSessionImpl(session)
	}

	override fun getSession(id: String): ISession? {
		val session = uninstaller.getSessionAsync(UUID.fromString(id)).get() ?: return null
		return RemoteSessionImpl(session)
	}
}