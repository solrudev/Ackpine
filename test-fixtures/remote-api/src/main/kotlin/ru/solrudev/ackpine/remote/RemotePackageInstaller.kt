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

import android.net.Uri
import ru.solrudev.ackpine.DelicateAckpineApi
import ru.solrudev.ackpine.installer.PackageInstaller
import ru.solrudev.ackpine.installer.createSession
import ru.solrudev.ackpine.installer.parameters.InstallerType
import ru.solrudev.ackpine.resources.ResolvableString
import ru.solrudev.ackpine.session.parameters.Confirmation
import ru.solrudev.ackpine.session.parameters.notification
import java.util.UUID

/**
 * A [PackageInstaller] bridge for IPC.
 */
public class RemotePackageInstaller internal constructor(private val installer: IPackageInstaller) {

	/**
	 * Creates an install [RemoteSession] with [Confirmation.IMMEDIATE] option.
	 */
	public fun createImmediateSession(
		type: InstallerType,
		uri: Uri,
		requireUserAction: Boolean = true
	): RemoteSession {
		val session = installer.createImmediateSession(
			type.name,
			uri.toString(),
			requireUserAction
		)
		return RemoteSession(session)
	}

	/**
	 * Creates an install [RemoteSession] with [Confirmation.DEFERRED] option.
	 */
	public fun createDeferredSession(
		type: InstallerType,
		uri: Uri,
		notificationTitle: String,
		requireUserAction: Boolean = true
	): RemoteSession {
		val session = installer.createDeferredSession(
			type.name,
			uri.toString(),
			notificationTitle,
			requireUserAction
		)
		return RemoteSession(session)
	}

	/**
	 * Returns an install [RemoteSession] which matches the provided [sessionId], or `null` if not found.
	 */
	public fun getSession(sessionId: UUID): RemoteSession? {
		val session = installer.getSession(sessionId.toString()) ?: return null
		return RemoteSession(session)
	}
}

@OptIn(DelicateAckpineApi::class)
internal class RemotePackageInstallerImpl(private val installer: PackageInstaller) : IPackageInstaller.Stub() {

	override fun createImmediateSession(
		type: String,
		uri: String,
		requireUserAction: Boolean
	): ISession {
		val session = installer.createSession(Uri.parse(uri)) {
			installerType = InstallerType.valueOf(type)
			confirmation = Confirmation.IMMEDIATE
			this.requireUserAction = requireUserAction
		}
		return RemoteSessionImpl(session)
	}

	override fun createDeferredSession(
		type: String,
		uri: String,
		notificationTitle: String,
		requireUserAction: Boolean
	): ISession {
		val session = installer.createSession(Uri.parse(uri)) {
			installerType = InstallerType.valueOf(type)
			confirmation = Confirmation.DEFERRED
			this.requireUserAction = requireUserAction
			notification {
				title = ResolvableString.raw(notificationTitle)
			}
		}
		return RemoteSessionImpl(session)
	}

	override fun getSession(id: String): ISession? {
		val session = installer.getSessionAsync(UUID.fromString(id)).get() ?: return null
		return RemoteSessionImpl(session)
	}
}