/*
 * Copyright (C) 2025 Ilya Fomichev
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

package ru.solrudev.ackpine.shizuku

import android.content.Context
import android.content.pm.IPackageInstaller
import android.content.pm.IPackageManager
import android.content.pm.PackageInstaller
import android.os.Build
import android.os.IBinder
import android.os.UserHandleHidden
import androidx.annotation.RestrictTo
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import ru.solrudev.ackpine.impl.services.PackageInstallerService
import ru.solrudev.ackpine.plugability.AckpinePlugin
import ru.solrudev.ackpine.privileged.PackageInstallerProxy
import java.util.UUID

/**
 * Implementation of [PackageInstallerService] which delegates work to [PackageInstaller] obtained through Shizuku.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class ShizukuPackageInstaller(
	packageInstaller: PackageInstaller,
	remotePackageInstaller: IPackageInstaller,
	uid: Int
) : PackageInstallerProxy(packageInstaller, remotePackageInstaller, uid) {

	override fun applyParameters(sessionId: UUID, parameters: AckpinePlugin.Parameters) {
		when (parameters) {
			is ShizukuPlugin.InstallParameters -> applyInstallParameters(sessionId, parameters)
			is ShizukuPlugin.UninstallParameters -> applyUninstallParameters(sessionId, parameters)
		}
	}

	override fun wrapBinder(original: IBinder): IBinder = ShizukuBinderWrapper(original)

	internal companion object Factory {

		@JvmSynthetic
		internal fun create(context: Context): ShizukuPackageInstaller {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
				HiddenApiBypass.addHiddenApiExemptions(
					"Landroid/content/pm/IPackageManager",
					"Landroid/content/pm/IPackageInstaller",
					"Landroid/content/pm/IPackageInstallerSession",
					"Landroid/content/pm/PackageInstaller",
					"Landroid/os/UserHandle"
				)
			}
			val remotePackageManager = IPackageManager.Stub.asInterface(
				ShizukuBinderWrapper(SystemServiceHelper.getSystemService("package"))
			)
			val remotePackageInstaller = IPackageInstaller.Stub.asInterface(
				ShizukuBinderWrapper(remotePackageManager.packageInstaller.asBinder())
			)
			val uid = Shizuku.getUid()
			val isRoot = uid == 0
			val installerPackageName = if (isRoot) context.packageName else "com.android.shell"
			return ShizukuPackageInstaller(
				createPackageInstaller(
					context,
					remotePackageInstaller,
					installerPackageName,
					UserHandleHidden.myUserId()
				),
				remotePackageInstaller,
				uid
			)
		}
	}
}