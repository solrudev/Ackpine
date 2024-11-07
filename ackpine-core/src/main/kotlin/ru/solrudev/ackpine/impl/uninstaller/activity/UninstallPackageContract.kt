/*
 * Copyright (C) 2023-2024 Ilya Fomichev
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

package ru.solrudev.ackpine.impl.uninstaller.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.annotation.RestrictTo
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.uninstaller.UninstallFailure

/**
 * Returns package uninstall [UninstallContract] for current API level.
 */
@Suppress("FunctionName")
@JvmSynthetic
internal fun UninstallPackageContract(packageName: String): UninstallContract {
	if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
		return ActionUninstallPackageContract(packageName)
	}
	return ActionDeletePackageContract(packageName)
}

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal interface UninstallContract {
	fun createIntent(context: Context): Intent
	fun parseResult(context: Context, resultCode: Int): Session.State.Completed<UninstallFailure>
}

private class ActionDeletePackageContract(private val packageName: String) : UninstallContract {

	override fun createIntent(context: Context): Intent {
		val packageUri = Uri.parse("package:$packageName")
		return Intent(Intent.ACTION_DELETE, packageUri)
	}

	override fun parseResult(context: Context, resultCode: Int): Session.State.Completed<UninstallFailure> {
		if (!context.isPackageInstalled(packageName)) {
			return Session.State.Succeeded
		}
		return Session.State.Failed(UninstallFailure.Generic)
	}

	private fun Context.isPackageInstalled(packageName: String) = try {
		packageManager.getPackageInfoCompat(packageName, PackageManager.GET_ACTIVITIES)
		true
	} catch (_: PackageManager.NameNotFoundException) {
		false
	}

	private fun PackageManager.getPackageInfoCompat(packageName: String, flags: Int): PackageInfo {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			return getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
		}
		return getPackageInfo(packageName, flags)
	}
}

private class ActionUninstallPackageContract(private val packageName: String) : UninstallContract {

	@Suppress("DEPRECATION")
	override fun createIntent(context: Context) = Intent().apply {
		action = Intent.ACTION_UNINSTALL_PACKAGE
		data = Uri.parse("package:$packageName")
		putExtra(Intent.EXTRA_RETURN_RESULT, true)
	}

	override fun parseResult(context: Context, resultCode: Int): Session.State.Completed<UninstallFailure> {
		return when (resultCode) {
			Activity.RESULT_OK -> Session.State.Succeeded
			Activity.RESULT_CANCELED -> Session.State.Failed(UninstallFailure.Aborted("Session was cancelled"))
			else -> Session.State.Failed(UninstallFailure.Generic)
		}
	}
}