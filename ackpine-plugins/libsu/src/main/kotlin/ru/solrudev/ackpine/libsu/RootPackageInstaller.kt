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

package ru.solrudev.ackpine.libsu

import android.content.Context
import android.content.pm.IPackageInstaller
import android.content.pm.IPackageManager
import android.os.IBinder
import android.os.ServiceManager
import androidx.annotation.RestrictTo
import ru.solrudev.ackpine.plugability.AckpinePlugin
import ru.solrudev.ackpine.privileged.PackageInstallerProxy
import java.util.UUID

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class RootPackageInstaller(
	private val rootService: IBinder,
	context: Context,
	remotePackageInstaller: IPackageInstaller
) : PackageInstallerProxy(context, remotePackageInstaller, context.packageName, uid = 0) {

	override fun applyParameters(sessionId: UUID, parameters: AckpinePlugin.Parameters) {
		when (parameters) {
			is LibsuPlugin.InstallParameters -> applyInstallParameters(sessionId, parameters)
			is LibsuPlugin.UninstallParameters -> applyUninstallParameters(sessionId, parameters)
		}
	}

	override fun wrapBinder(original: IBinder) = RootProxyBinderWrapper(rootService, original)

	internal companion object Factory {

		@JvmSynthetic
		internal fun create(context: Context): RootPackageInstaller {
			val rootService = RootProxyService.bind(context)
			val remotePackageManager = IPackageManager.Stub.asInterface(
				RootProxyBinderWrapper(rootService, ServiceManager.getService("package"))
			)
			val remotePackageInstaller = IPackageInstaller.Stub.asInterface(
				RootProxyBinderWrapper(rootService, remotePackageManager.packageInstaller.asBinder())
			)
			return RootPackageInstaller(
				rootService,
				context,
				remotePackageInstaller
			)
		}
	}
}