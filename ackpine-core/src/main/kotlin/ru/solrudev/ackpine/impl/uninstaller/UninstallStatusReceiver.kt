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
import ru.solrudev.ackpine.impl.session.CompletableSession
import ru.solrudev.ackpine.impl.uninstaller.activity.PackageInstallerBasedUninstallActivity
import ru.solrudev.ackpine.impl.uninstaller.activity.UninstallActivity
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.uninstaller.UninstallFailure
import ru.solrudev.ackpine.uninstaller.UninstallFailure.Aborted
import ru.solrudev.ackpine.uninstaller.UninstallFailure.Blocked
import ru.solrudev.ackpine.uninstaller.UninstallFailure.Conflict
import ru.solrudev.ackpine.uninstaller.UninstallFailure.Generic
import ru.solrudev.ackpine.uninstaller.parameters.PackageVersionCode
import ru.solrudev.ackpine.uninstaller.parameters.VersionedUninstallPolicy
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
		val packageName = intent.getStringExtra(UninstallActivity.EXTRA_PACKAGE_NAME)
		return wrapperIntent
			.putExtra(UninstallActivity.EXTRA_PACKAGE_NAME, packageName)
			.putExtra(
				UninstallActivity.EXTRA_PACKAGE_VERSION_CODE,
				intent.getLongExtra(UninstallActivity.EXTRA_PACKAGE_VERSION_CODE, PackageVersionCode.HIGHEST.value)
			)
			.putExtra(
				UninstallActivity.EXTRA_VERSIONED_UNINSTALL_POLICY,
				intent.getIntExtra(
					UninstallActivity.EXTRA_VERSIONED_UNINSTALL_POLICY,
					VersionedUninstallPolicy.STRICT.ordinal
				)
			)
	}

	override fun onUserActionRequired(
		session: CompletableSession<UninstallFailure>,
		intent: Intent
	): Boolean {
		val version = intent.getLongExtra(
			UninstallActivity.EXTRA_PACKAGE_VERSION_CODE,
			PackageVersionCode.HIGHEST.value
		)
		val policy = intent.getIntExtra(
			UninstallActivity.EXTRA_VERSIONED_UNINSTALL_POLICY,
			VersionedUninstallPolicy.STRICT.ordinal
		)
		// AOSP drops the version constraint when creating the uninstall confirmation intent.
		// Reject this path under STRICT because a replacement version could otherwise be removed.
		// ref: https://cs.android.com/android/_/android/platform/frameworks/base/+/cbb3299c41a06cf28e31d85d3ac2418368de8a86:services/core/java/com/android/server/pm/PackageInstallerService.java;l=1666;drc=cbb3299c41a06cf28e31d85d3ac2418368de8a86
		if (version != PackageVersionCode.HIGHEST.value && policy == VersionedUninstallPolicy.STRICT.ordinal) {
			session.complete(
				Session.State.Failed(
					UninstallFailure.Unsupported("Exact version uninstall requires a non-confirmation path")
				)
			)
			return false
		}
		return true
	}

	@RestrictTo(RestrictTo.Scope.LIBRARY)
	internal companion object {
		@JvmSynthetic
		internal fun getAction(context: Context) = "${context.packageName}.PACKAGE_UNINSTALLER_STATUS"
	}
}