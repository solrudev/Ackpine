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

package ru.solrudev.ackpine.dhizuku

import android.content.Context
import android.content.pm.IPackageInstaller
import android.content.pm.IPackageManager
import android.content.pm.PackageInstaller
import android.os.IBinder
import android.os.ServiceManager
import androidx.annotation.RestrictTo
import com.rosan.dhizuku.api.Dhizuku
import ru.solrudev.ackpine.impl.services.PackageInstallerService
import ru.solrudev.ackpine.plugability.AckpinePlugin
import ru.solrudev.ackpine.privileged.PackageInstallerProxy
import ru.solrudev.ackpine.privileged.PrivilegedInstallParameters
import ru.solrudev.ackpine.privileged.PrivilegedUninstallParameters
import ru.solrudev.ackpine.privileged.TargetUser
import java.util.UUID

/**
 * Implementation of [PackageInstallerService] which delegates work to [PackageInstaller] obtained through Dhizuku.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class DhizukuPackageInstaller(
	context: Context,
	remotePackageInstaller: IPackageInstaller,
	ownerPackageName: String,
	ownerUid: Int
) : PackageInstallerProxy(context, remotePackageInstaller, ownerPackageName, ownerUid) {

	override fun applyParameters(sessionId: UUID, parameters: AckpinePlugin.Parameters) {
		when (parameters) {
			is DhizukuPlugin.InstallParameters -> applyInstallParameters(
				sessionId,
				DhizukuInstallParametersBridge.create(parameters)
			)

			is DhizukuPlugin.UninstallParameters -> applyUninstallParameters(
				sessionId,
				DhizukuUninstallParametersBridge.create(parameters)
			)
		}
	}

	override fun wrapBinder(original: IBinder): IBinder = Dhizuku.binderWrapper(original)

	internal companion object Factory {

		@JvmSynthetic
		internal fun create(context: Context): DhizukuPackageInstaller {
			check(Dhizuku.init(context)) { "Failed to initialize Dhizuku" }
			val remotePackageManager = IPackageManager.Stub.asInterface(
				Dhizuku.binderWrapper(ServiceManager.getService("package"))
			)
			val remotePackageInstaller = IPackageInstaller.Stub.asInterface(
				Dhizuku.binderWrapper(remotePackageManager.packageInstaller.asBinder())
			)
			val ownerPackageName = Dhizuku.getOwnerPackageName()
			val ownerUid = context.packageManager.getApplicationInfo(ownerPackageName, 0).uid
			return DhizukuPackageInstaller(
				context,
				remotePackageInstaller,
				ownerPackageName,
				ownerUid
			)
		}
	}
}

private class DhizukuInstallParametersBridge private constructor(
	snapshot: Snapshot
) : PrivilegedInstallParameters(snapshot) {

	override fun getName(): String = "DhizukuInstallParametersBridge"

	private class Builder : PrivilegedInstallParameters.Builder<DhizukuInstallParametersBridge, Builder>() {
		override fun build(): DhizukuInstallParametersBridge = DhizukuInstallParametersBridge(buildSnapshot())
	}

	companion object {
		fun create(parameters: DhizukuPlugin.InstallParameters): DhizukuInstallParametersBridge = Builder()
			.setRequestDowngrade(parameters.requestDowngrade)
			.setTargetUser(TargetUser.CURRENT)
			.build()
	}
}

private class DhizukuUninstallParametersBridge private constructor(
	snapshot: Snapshot
) : PrivilegedUninstallParameters(snapshot) {

	override fun getName(): String = "DhizukuUninstallParametersBridge"

	private class Builder : PrivilegedUninstallParameters.Builder<DhizukuUninstallParametersBridge, Builder>() {
		override fun build(): DhizukuUninstallParametersBridge = DhizukuUninstallParametersBridge(buildSnapshot())
	}

	companion object {
		fun create(parameters: DhizukuPlugin.UninstallParameters): DhizukuUninstallParametersBridge = Builder()
			.setKeepData(parameters.keepData)
			.setAllUsers(parameters.allUsers)
			.setSystemApp(parameters.systemApp)
			.setTargetUser(TargetUser.CURRENT)
			.build()
	}
}