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

package ru.solrudev.ackpine.impl.installer

import android.content.Context
import android.content.pm.PackageInstaller
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import ru.solrudev.ackpine.impl.installer.activity.SessionBasedInstallConfirmationActivity
import ru.solrudev.ackpine.impl.receiver.PackageInstallerStatusReceiver
import ru.solrudev.ackpine.installer.InstallFailure
import java.util.UUID

@RestrictTo(RestrictTo.Scope.LIBRARY)
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
internal class InstallStatusReceiver : PackageInstallerStatusReceiver<InstallFailure>(
	confirmationWrapperActivityClass = SessionBasedInstallConfirmationActivity::class.java,
	tag = "InstallStatusReceiver"
) {

	override fun getAckpineSessionAsync(
		context: Context,
		ackpineSessionId: UUID
	) = PackageInstallerImpl.getInstance(context).getSessionAsync(ackpineSessionId)

	override fun getFailure(
		status: Int,
		message: String?,
		otherPackageName: String?,
		storagePath: String?
	) = when (status) {
		PackageInstaller.STATUS_FAILURE -> InstallFailure.Generic(message)
		PackageInstaller.STATUS_FAILURE_ABORTED -> InstallFailure.Aborted(message)
		PackageInstaller.STATUS_FAILURE_BLOCKED -> InstallFailure.Blocked(message, otherPackageName)
		PackageInstaller.STATUS_FAILURE_CONFLICT -> InstallFailure.Conflict(message, otherPackageName)
		PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> InstallFailure.Incompatible(message)
		PackageInstaller.STATUS_FAILURE_INVALID -> InstallFailure.Invalid(message)
		PackageInstaller.STATUS_FAILURE_STORAGE -> InstallFailure.Storage(message, storagePath)
		PackageInstaller.STATUS_FAILURE_TIMEOUT -> InstallFailure.Timeout(message)
		else -> InstallFailure.Generic("Unknown failure")
	}

	override fun getAction(context: Context) = Companion.getAction(context)

	@RestrictTo(RestrictTo.Scope.LIBRARY)
	internal companion object {
		@JvmSynthetic
		internal fun getAction(context: Context) = "${context.packageName}.PACKAGE_INSTALLER_STATUS"
	}
}