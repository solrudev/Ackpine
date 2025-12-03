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

package ru.solrudev.ackpine.impl.uninstaller

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import ru.solrudev.ackpine.impl.receiver.SystemPackageInstallerStatusReceiver
import ru.solrudev.ackpine.impl.uninstaller.activity.PackageInstallerBasedUninstallActivity
import ru.solrudev.ackpine.uninstaller.UninstallFailure
import ru.solrudev.ackpine.uninstaller.UninstallFailure.Aborted
import ru.solrudev.ackpine.uninstaller.UninstallFailure.Blocked
import ru.solrudev.ackpine.uninstaller.UninstallFailure.Conflict
import ru.solrudev.ackpine.uninstaller.UninstallFailure.Generic
import java.util.UUID

@RestrictTo(RestrictTo.Scope.LIBRARY)
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
internal class UninstallStatusReceiver : SystemPackageInstallerStatusReceiver<UninstallFailure>(
	confirmationWrapperActivityClass = PackageInstallerBasedUninstallActivity::class.java,
	tag = "UninstallStatusReceiver"
) {

	override fun getAckpineSessionAsync(
		context: Context,
		ackpineSessionId: UUID
	) = PackageUninstallerImpl.getInstance(context).getSessionAsync(ackpineSessionId)

	override fun getFailure(
		status: Int,
		message: String?,
		otherPackageName: String?,
		storagePath: String?
	) = when (status) {
		PackageInstaller.STATUS_FAILURE -> Generic(message)
		PackageInstaller.STATUS_FAILURE_ABORTED -> Aborted(message)
		PackageInstaller.STATUS_FAILURE_BLOCKED -> Blocked(message, otherPackageName)
		PackageInstaller.STATUS_FAILURE_CONFLICT -> Conflict(message, otherPackageName)
		else -> Generic("Unknown failure")
	}

	override fun getAction(context: Context) = Companion.getAction(context)

	override fun modifyConfirmationWrapperIntent(intent: Intent, wrapperIntent: Intent): Intent {
		val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME)
		return wrapperIntent.putExtra(EXTRA_PACKAGE_NAME, packageName)
	}

	@RestrictTo(RestrictTo.Scope.LIBRARY)
	internal companion object {

		@JvmSynthetic
		internal fun getAction(context: Context) = "${context.packageName}.PACKAGE_UNINSTALLER_STATUS"

		@JvmSynthetic
		internal const val EXTRA_PACKAGE_NAME = "ru.solrudev.ackpine.extra.PACKAGE_NAME"
	}
}