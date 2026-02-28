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

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.solrudev.ackpine.DelicateAckpineApi
import ru.solrudev.ackpine.installer.PackageInstaller
import ru.solrudev.ackpine.installer.createSession
import ru.solrudev.ackpine.installer.parameters.InstallerType
import ru.solrudev.ackpine.installer.parameters.preapproval
import ru.solrudev.ackpine.remote.dsl.RemoteInstallParameters
import ru.solrudev.ackpine.remote.dsl.RemoteInstallParametersDsl
import ru.solrudev.ackpine.remote.dsl.RemoteInstallParametersDslBuilder
import ru.solrudev.ackpine.resources.ResolvableString
import ru.solrudev.ackpine.session.parameters.Confirmation
import ru.solrudev.ackpine.session.parameters.notification
import java.util.UUID

/**
 * A [PackageInstaller] bridge for IPC.
 */
public class RemotePackageInstaller internal constructor(private val installer: IPackageInstaller) {

	/**
	 * Creates an install [RemoteSession] configured via [DSL][RemoteInstallParametersDsl]. The returned session is in
	 * [pending][RemoteSession.State.Pending] state.
	 */
	public inline fun createSession(
		baseApk: Uri,
		configure: RemoteInstallParametersDsl.() -> Unit = {}
	): RemoteSession {
		val parameters = RemoteInstallParametersDslBuilder(baseApk).apply(configure).build()
		return createSession(parameters)
	}

	/**
	 * Creates an install [RemoteSession] configured via [DSL][RemoteInstallParametersDsl]. The returned session is in
	 * [pending][RemoteSession.State.Pending] state.
	 */
	@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
	public inline fun createSession(
		apks: Iterable<Uri>,
		configure: RemoteInstallParametersDsl.() -> Unit = {}
	): RemoteSession {
		val parameters = RemoteInstallParametersDslBuilder(apks).apply(configure).build()
		return createSession(parameters)
	}

	@PublishedApi
	internal fun createSession(parameters: RemoteInstallParameters): RemoteSession = with(parameters) {
		val session = installer.createSession(
			installParameters.installerType.ordinal,
			installParameters.apks.toList().map { it.toString() },
			installParameters.confirmation.ordinal,
			notificationData.title,
			notificationData.contentText,
			installParameters.preapproval.packageName,
			installParameters.preapproval.label,
			installParameters.preapproval.languageTag,
			installParameters.preapproval.icon.toString(),
			installParameters.preapproval.fallbackToOnDemandApproval,
			installParameters.requireUserAction
		)
		RemoteSession(session)
	}

	/**
	 * Returns an install [RemoteSession] which matches the provided [sessionId], or `null` if not found.
	 */
	public suspend fun getSession(sessionId: UUID): RemoteSession? = withContext(Dispatchers.IO) {
		installer.getSession(sessionId.toString())?.let(::RemoteSession)
	}
}

@OptIn(DelicateAckpineApi::class)
internal class RemotePackageInstallerImpl(private val installer: PackageInstaller) : IPackageInstaller.Stub() {

	@SuppressLint("NewApi")
	override fun createSession(
		type: Int,
		uri: MutableList<String>,
		confirmation: Int,
		notificationTitle: String,
		notificationText: String,
		preapprovalPackageName: String,
		preapprovalLabel: String,
		preapprovalLanguageTag: String,
		preapprovalIconUri: String,
		fallbackToOnDemandApproval: Boolean,
		requireUserAction: Boolean
	): ISession {
		val session = installer.createSession(uri.map(Uri::parse)) {
			installerType = InstallerType.entries[type]
			this.confirmation = Confirmation.entries[confirmation]
			this.requireUserAction = requireUserAction
			notification {
				if (notificationTitle.isNotEmpty()) {
					title = ResolvableString.raw(notificationTitle)
				}
				if (notificationText.isNotEmpty()) {
					contentText = ResolvableString.raw(notificationText)
				}
			}
			if (preapprovalPackageName.isNotEmpty() && preapprovalLabel.isNotEmpty()) {
				preapproval(preapprovalPackageName, preapprovalLabel, preapprovalLanguageTag) {
					icon = Uri.parse(preapprovalIconUri)
					this.fallbackToOnDemandApproval = fallbackToOnDemandApproval
				}
			}
		}
		return RemoteSessionImpl(session)
	}

	override fun getSession(id: String): ISession? {
		val session = installer.getSessionAsync(UUID.fromString(id)).get() ?: return null
		return RemoteSessionImpl(session)
	}
}